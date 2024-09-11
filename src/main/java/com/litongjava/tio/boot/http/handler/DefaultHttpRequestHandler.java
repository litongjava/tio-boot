package com.litongjava.tio.boot.http.handler;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.tio.boot.constatns.TioBootConfigKeys;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoute;
import com.litongjava.tio.boot.http.session.SessionLimit;
import com.litongjava.tio.boot.http.utils.TioHttpHandlerUtil;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpMethod;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.server.handler.HttpRequestRouteHandler;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.intf.HttpSessionListener;
import com.litongjava.tio.http.server.intf.ThrowableHandler;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.router.HttpReqeustGroovyRoute;
import com.litongjava.tio.http.server.router.RequestRoute;
import com.litongjava.tio.http.server.session.HttpSessionUtils;
import com.litongjava.tio.http.server.session.SessionCookieDecorator;
import com.litongjava.tio.http.server.stat.ip.path.IpPathAccessStats;
import com.litongjava.tio.http.server.stat.token.TokenPathAccessStats;
import com.litongjava.tio.http.server.util.HttpServerResponseUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.SysConst;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCache;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.ArrayUtil;
import com.litongjava.tio.utils.hutool.StrUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author litongjava
 */
@Slf4j
public class DefaultHttpRequestHandler implements HttpRequestHandler {

  private static final Map<Class<?>, MethodAccess> CLASS_METHODACCESS_MAP = new HashMap<>();
  protected HttpConfig httpConfig;
  protected TioBootHttpControllerRoute controllerRoutes = null;
  private RequestRoute simpleHandlerRoute;
  private HttpReqeustGroovyRoute groovyRoutes;
  private HttpRequestInterceptor httpRequestInterceptor;
  private HttpSessionListener httpSessionListener;
  private ThrowableHandler throwableHandler;
  private SessionCookieDecorator sessionCookieDecorator;
  private IpPathAccessStats ipPathAccessStats;
  private TokenPathAccessStats tokenPathAccessStats;
  private RequestStatisticsHandler requestStatisticsHandler;
  private ResponseStatisticsHandler responseStatisticsHandler;
  private StaticResourceHandler staticResourceHandler;
  private HttpNotFoundHandler notFoundHandler;
  private DynamicRequestHandler dynamicRequestHandler;
  private AccessStatisticsHandler accessStatisticsHandler = new AccessStatisticsHandler();
  /**
   * 静态资源缓存
   */
  private AbsCache staticResCache;
  /**
   * 限流缓存
   */
  private AbsCache sessionRateLimiterCache;

  private String contextPath;
  private int contextPathLength = 0;
  private String suffix;
  private int suffixLength = 0;
  /**
   * 赋值兼容处理
   */
  private boolean compatibilityAssignment = true;

  public void init(HttpConfig httpConfig, TioBootHttpControllerRoute tioBootHttpControllerRoutes, HttpRequestInterceptor defaultHttpServerInterceptorDispather,
      //
      RequestRoute httpReqeustSimpleHandlerRoute, HttpReqeustGroovyRoute httpReqeustGroovyRoute,
      //
      ConcurrentMapCacheFactory cacheFactory,
      //
      HttpNotFoundHandler notFoundHandler, RequestStatisticsHandler requestStatisticsHandler, ResponseStatisticsHandler responseStatisticsHandler) {

    this.controllerRoutes = tioBootHttpControllerRoutes;

    this.httpRequestInterceptor = defaultHttpServerInterceptorDispather;
    this.simpleHandlerRoute = httpReqeustSimpleHandlerRoute;
    this.groovyRoutes = httpReqeustGroovyRoute;
    this.notFoundHandler = notFoundHandler;
    this.requestStatisticsHandler = requestStatisticsHandler;
    this.responseStatisticsHandler = responseStatisticsHandler;

    if (httpConfig == null) {
      throw new RuntimeException("httpConfig can not be null");
    }
    this.contextPath = httpConfig.getContextPath();
    this.suffix = httpConfig.getSuffix();

    if (StrUtil.isNotBlank(contextPath)) {
      contextPathLength = contextPath.length();
    }
    if (StrUtil.isNotBlank(suffix)) {
      suffixLength = suffix.length();
    }

    this.httpConfig = httpConfig;

    if (httpConfig.getMaxLiveTimeOfStaticRes() > 0) {
      long maxLiveTimeOfStaticRes = (long) httpConfig.getMaxLiveTimeOfStaticRes();
      staticResCache = cacheFactory.register(DefaultHttpRequestConstants.STATIC_RES_CONTENT_CACHENAME, maxLiveTimeOfStaticRes, null);
    }

    sessionRateLimiterCache = cacheFactory.register(DefaultHttpRequestConstants.SESSION_RATE_LIMITER_CACHENAME, 60 * 1L, null);

    if (httpConfig.getPageRoot() != null) {
      try {
        this.monitorFileChanged();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    staticResourceHandler = new StaticResourceHandler(httpConfig, staticResCache);
    dynamicRequestHandler = new DynamicRequestHandler();

  }

  /**
   * 创建httpsession
   *
   * @return
   */
  public HttpSession createSession(HttpRequest request) {
    String sessionId = httpConfig.getSessionIdGenerator().sessionId(httpConfig, request);
    HttpSession httpSession = new HttpSession(sessionId);
    if (httpSessionListener != null) {
      httpSessionListener.doAfterCreated(request, httpSession, httpConfig);
    }
    return httpSession;
  }

  /**
   * @return the httpConfig
   */
  public HttpConfig getHttpConfig(HttpRequest request) {
    return httpConfig;
  }

  public HttpRequestInterceptor getHttpServerInterceptor() {
    return httpRequestInterceptor;
  }

  /**
   * @return the staticResCache
   */
  public AbsCache getStaticResCache() {
    return staticResCache;
  }

  /**
   * 检查域名是否可以访问本站
   *
   * @param request
   * @return
   */
  private boolean checkDomain(HttpRequest request) {
    String[] allowDomains = httpConfig.getAllowDomains();
    if (allowDomains == null || allowDomains.length == 0) {
      return true;
    }
    String host = request.getHost();
    if (ArrayUtil.contains(allowDomains, host)) {
      return true;
    }
    return false;
  }

  @Override
  public HttpResponse handler(HttpRequest request) throws Exception {

    request.setNeedForward(false);

    // check domain
    if (!checkDomain(request)) {
      Tio.remove(request.channelContext, "Incorrect domain name" + request.getDomain());
      return null;
    }

    long start = SystemTimer.currTime;

    RequestLine requestLine = request.getRequestLine();
    String path = requestLine.path;

    if (StrUtil.isNotBlank(contextPath)) {
      if (StrUtil.startWith(path, contextPath)) {
        path = StrUtil.subSuf(path, contextPathLength);
      }
    }

    if (StrUtil.isNotBlank(suffix)) {
      if (StrUtil.endWith(path, suffix)) {
        path = StrUtil.sub(path, 0, path.length() - suffixLength);
      } else {

      }
    }
    requestLine.setPath(path);

    processCookieBeforeHandler(request, requestLine);

    HttpResponse httpResponse = null;
    // print url
    if (EnvUtils.getBoolean(TioBootConfigKeys.TIO_HTTP_REQUEST_PRINT_URL)) {
      log.info("access:{}:{}", requestLine.getMethod().toString(), path);
    }

    // 流控
    if (httpConfig.isUseSession()) {
      httpResponse = SessionLimit.build(request, path, httpConfig, sessionRateLimiterCache);
      if (httpResponse != null) {
        return httpResponse;
      }
    }

    // options 无须统计
    if (requestLine.method.equals(HttpMethod.OPTIONS)) { // allow all OPTIONS request
      httpResponse = new HttpResponse(request);
      HttpServerResponseUtils.enableCORS(httpResponse, new HttpCors());
      return httpResponse;
    }

    // 接口访问统计
    if (requestStatisticsHandler != null) {
      requestStatisticsHandler.count(request);
    }

    requestLine = request.getRequestLine();
    path = requestLine.path;
    boolean printReport = EnvUtils.getBoolean("tio.mvc.request.printReport", false);

    try {
      TioRequestContext.hold(request);
      // Interceptor
      httpResponse = httpRequestInterceptor.doBeforeHandler(request, requestLine, httpResponse);
      if (httpResponse != null) {
        if (printReport) {
          if (log.isInfoEnabled()) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("\n-----------httpRequestInterceptor report---------------------\n");
            stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                .append("httpServerInterceptor:" + httpRequestInterceptor).append("\n")//
                .append("response:" + httpResponse).append("\n")//
                .append("\n");

            log.info(stringBuffer.toString());

          }
        }
        return httpResponse;
      }

      // simpleHandlerRoute
      HttpRequestRouteHandler httpRequestRouteHandler = simpleHandlerRoute.find(path);
      if (httpRequestRouteHandler != null) {
        if (printReport) {
          StringBuffer stringBuffer = new StringBuffer();
          stringBuffer.append("\n-----------httpRequestRouteHandler report---------------------\n");
          stringBuffer.append("request:" + requestLine.toString()).append("\n")//
              .append("handler:" + httpRequestRouteHandler.toString()).append("\n");
          log.info(stringBuffer.toString());
        }
        httpResponse = httpRequestRouteHandler.handle(request);
      }

      // groovyRoutes
      if (groovyRoutes != null) {
        httpRequestRouteHandler = groovyRoutes.find(path);
        if (httpRequestRouteHandler != null) {
          if (printReport) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("\n-----------groovyRouteHandler report---------------------\n");
            stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                .append("handler:" + httpRequestRouteHandler.toString()).append("\n");
            log.info(stringBuffer.toString());
          }
          httpResponse = httpRequestRouteHandler.handle(request);
        }
      }

      Method method = TioHttpHandlerUtil.getActionMethod(httpConfig, controllerRoutes, request, requestLine);
      // 执行动态请求
      if (httpResponse == null && method != null) {
        if (printReport) {
          if (log.isInfoEnabled()) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("\n-----------action report---------------------\n");
            stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                .append("method:" + method.toString()).append("\n");
            log.info(stringBuffer.toString());
          }
        }
        httpResponse = dynamicRequestHandler.processDynamic(httpConfig, controllerRoutes, compatibilityAssignment, CLASS_METHODACCESS_MAP, request, method);
        if (httpResponse != null) {
          return httpResponse;
        }
      }

      // 请求静态文件
      if (method == null) {
        httpResponse = staticResourceHandler.processStatic(path, request);
        if (httpResponse != null) {
          return httpResponse;
        } else {
          httpResponse = resp404(request, requestLine);
          if (httpResponse != null) {
            return httpResponse;
          }
        }
      }

      return httpResponse;
    } catch (Throwable e) {
      httpResponse = resp500(request, requestLine, e);
      return httpResponse;
    } finally {
      Object userId = TioRequestContext.getUserId();

      TioRequestContext.release();
      long time = SystemTimer.currTime;
      long iv = time - start; // 本次请求消耗的时间，单位：毫秒

      try {
        processCookieAfterHandler(request, requestLine, httpResponse);
      } catch (Exception e) {
        log.error(requestLine.toString(), e);
        e.printStackTrace();
      }

      if (httpRequestInterceptor != null) {
        try {
          httpRequestInterceptor.doAfterHandler(request, requestLine, httpResponse, iv);
        } catch (Exception e) {
          log.error(requestLine.toString(), e);
          e.printStackTrace();
        }
      }

      if (ipPathAccessStats != null) {
        accessStatisticsHandler.statIpPath(ipPathAccessStats, request, httpResponse, path, iv);
      }

      if (tokenPathAccessStats != null) {
        accessStatisticsHandler.statTokenPath(tokenPathAccessStats, request, httpResponse, path, iv);
      }

      if (request.isNeedForward()) {
        request.setForward(true);
        return handler(request);
      } else {
        if (responseStatisticsHandler != null) {
          try {
            this.responseStatisticsHandler.count(request, requestLine, httpResponse, userId, iv);
          } catch (Exception e) {
            e.printStackTrace();
          }

        }

      }
    }

  }

  /**
   * 扫描文件变化
   *
   * @throws Exception
   */
  public void monitorFileChanged() throws Exception {
    if (httpConfig.monitorFileChange) {
      if (httpConfig.getPageRoot() != null) {
        File directory = new File(httpConfig.getPageRoot());// 需要扫描的文件夹路径
        // 测试采用轮询间隔 5 秒
        long interval = TimeUnit.SECONDS.toMillis(5);
        FileAlterationObserver observer = new FileAlterationObserver(directory, new FileFilter() {
          @Override
          public boolean accept(File pathname) {
            return true;
          }
        });
        // 设置文件变化监听器
        // observer.addListener(new FileChangeListener(this));
        FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
        monitor.start();
      }
    }
  }

  private void processCookieAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse) {
    if (httpResponse == null) {
      return;
    }

    if (!httpConfig.isUseSession()) {
      return;
    }

    HttpSession httpSession = request.getHttpSession();
    String sessionId = HttpSessionUtils.getSessionId(request);
    if (StrUtil.isBlank(sessionId)) {
      createSessionCookie(request, httpSession, httpResponse, false);
      // log.info("{} 创建会话Cookie, {}", request.getChannelContext(), cookie);
    } else {
      HttpSession httpSessionFromStroe = (HttpSession) httpConfig.getSessionStore().get(sessionId);
      if (httpSessionFromStroe == null) {// 有cookie但是超时了
        createSessionCookie(request, httpSession, httpResponse, false);
      }
    }
  }

  /**
   * 根据session创建session对应的cookie 注意：先有session，后有session对应的cookie
   *
   * @param request
   * @param httpSession
   * @param httpResponse
   * @param forceCreate
   * @return
   */
  private void createSessionCookie(HttpRequest request, HttpSession httpSession, HttpResponse httpResponse, boolean forceCreate) {
    if (httpResponse == null) {
      return;
    }

    if (!forceCreate) {
      Object test = request.channelContext.getAttribute(DefaultHttpRequestConstants.SESSION_COOKIE_KEY);
      if (test != null) {
        return;
      }
    }

    String sessionId = httpSession.getId();
    String domain = TioHttpHandlerUtil.getDomain(request);
    String name = httpConfig.getSessionCookieName();
    long maxAge = 3600 * 24 * 365 * 10;// Math.max(httpConfig.getSessionTimeout() * 30, 3600 * 24 * 365 * 10);

    Cookie sessionCookie = new Cookie(domain, name, sessionId, maxAge);
    if (sessionCookieDecorator != null) {
      sessionCookieDecorator.decorate(sessionCookie, request, request.getDomain());
    }
    httpResponse.addCookie(sessionCookie);

    httpConfig.getSessionStore().put(sessionId, httpSession);
    request.channelContext.setAttribute(DefaultHttpRequestConstants.SESSION_COOKIE_KEY, sessionCookie);
    return;
  }

  /**
   * 更新sessionId
   *
   * @param request
   * @param httpSession
   * @param httpResponse
   * @return
   */
  public HttpSession updateSessionId(HttpRequest request, HttpSession httpSession, HttpResponse httpResponse) {
    String oldId = httpSession.getId();
    String newId = httpConfig.getSessionIdGenerator().sessionId(httpConfig, request);
    httpSession.setId(newId);

    if (httpSessionListener != null) {
      httpSessionListener.doAfterCreated(request, httpSession, httpConfig);
    }
    httpConfig.getSessionStore().remove(oldId);
    createSessionCookie(request, httpSession, httpResponse, true);
    httpSession.update(httpConfig); // HttpSession有变动时，都要调一下update()
    return httpSession;
  }

  private void processCookieBeforeHandler(HttpRequest request, RequestLine requestLine) throws ExecutionException {
    if (!httpConfig.isUseSession()) {
      return;
    }

    String sessionId = HttpSessionUtils.getSessionId(request);
    // Cookie cookie = getSessionCookie(request, httpConfig);
    HttpSession httpSession = null;
    if (StrUtil.isBlank(sessionId)) {
      httpSession = createSession(request);
    } else {
      // if (StrUtil.isBlank(sessionId)) {
      // sessionId = cookie.getValue();
      // }

      httpSession = (HttpSession) httpConfig.getSessionStore().get(sessionId);
      if (httpSession == null) {
        if (log.isDebugEnabled()) {
          log.info("{} session {} timeout", request.channelContext, sessionId);
        }

        httpSession = createSession(request);
      }
    }
    request.setHttpSession(httpSession);
  }

  @Override
  public HttpResponse resp404(HttpRequest request, RequestLine requestLine) throws Exception {
    if (notFoundHandler != null) {
      return notFoundHandler.handle(request);
    }
    if (controllerRoutes != null) {
      String page404 = httpConfig.getPage404();
      Method method = controllerRoutes.PATH_METHOD_MAP.get(page404);
      if (method != null) {
        return Resps.forward(request, page404);
      }
    }

    return Resps.resp404(request, requestLine, httpConfig);
  }

  @Override
  public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {

    if (throwableHandler != null) {
      return throwableHandler.handler(request, requestLine, throwable);
    }

    String page500 = httpConfig.getPage500();
    TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();

    if (page500 != null) {
      if (exceptionHandler != null) {
        exceptionHandler.handler(request, throwable);
      }
      if (controllerRoutes != null) {
        Method method = controllerRoutes.PATH_METHOD_MAP.get(page500);
        if (method != null) {
          return Resps.forward(request, page500);
        }
      }
    }

    if (exceptionHandler != null) {
      Object result = exceptionHandler.handler(request, throwable);
      if (result != null) {
        HttpResponse response = TioRequestContext.getResponse();
        response.setStatus(500);
        return response.setJson(result);
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append(SysConst.CRLF).append("remote  :").append(request.getClientIp());
    sb.append(SysConst.CRLF).append("request :").append(requestLine.toString());
    log.error(sb.toString(), throwable);
    log.error(sb.toString(), throwable);
    return Resps.resp500(request, requestLine, httpConfig, throwable);
  }

  /**
   * @param httpConfig the httpConfig to set
   */
  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public void setHttpServerInterceptor(HttpRequestInterceptor httpServerInterceptor) {
    this.httpRequestInterceptor = httpServerInterceptor;
  }

  /**
   * @param staticResCache the staticResCache to set
   */
  public void setStaticResCache(CaffeineCache staticResCache) {
    this.staticResCache = staticResCache;
  }

  @Override
  public void clearStaticResCache() {
    if (staticResCache != null) {
      staticResCache.clear();
    }
  }

  public HttpSessionListener getHttpSessionListener() {
    return httpSessionListener;
  }

  public void setHttpSessionListener(HttpSessionListener httpSessionListener) {
    this.httpSessionListener = httpSessionListener;
  }

  public SessionCookieDecorator getSessionCookieDecorator() {
    return sessionCookieDecorator;
  }

  public void setSessionCookieDecorator(SessionCookieDecorator sessionCookieDecorator) {
    this.sessionCookieDecorator = sessionCookieDecorator;
  }

  public IpPathAccessStats getIpPathAccessStats() {
    return ipPathAccessStats;
  }

  public void setIpPathAccessStats(IpPathAccessStats ipPathAccessStats) {
    this.ipPathAccessStats = ipPathAccessStats;
  }

  public ThrowableHandler getThrowableHandler() {
    return throwableHandler;
  }

  public void setThrowableHandler(ThrowableHandler throwableHandler) {
    this.throwableHandler = throwableHandler;
  }

  public TokenPathAccessStats getTokenPathAccessStats() {
    return tokenPathAccessStats;
  }

  public void setTokenPathAccessStats(TokenPathAccessStats tokenPathAccessStats) {
    this.tokenPathAccessStats = tokenPathAccessStats;
  }

  public boolean isCompatibilityAssignment() {
    return compatibilityAssignment;
  }

  public void setCompatibilityAssignment(boolean compatibilityAssignment) {
    this.compatibilityAssignment = compatibilityAssignment;
  }

}
