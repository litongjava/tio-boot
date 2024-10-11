package com.litongjava.tio.boot.http.handler;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.litongjava.constatns.ServerConfigKeys;
import com.litongjava.model.sys.SysConst;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.http.router.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.http.session.SessionLimit;
import com.litongjava.tio.boot.http.utils.TioHttpControllerUtils;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpMethod;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.handler.RouteEntry;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.intf.HttpSessionListener;
import com.litongjava.tio.http.server.intf.ThrowableHandler;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.router.HttpReqeustGroovyRouter;
import com.litongjava.tio.http.server.router.HttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.http.server.session.HttpSessionUtils;
import com.litongjava.tio.http.server.session.SessionCookieDecorator;
import com.litongjava.tio.http.server.stat.ip.path.IpPathAccessStats;
import com.litongjava.tio.http.server.stat.token.TokenPathAccessStats;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.ArrayUtil;
import com.litongjava.tio.utils.hutool.StrUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author litongjava
 */
@Slf4j
public class TioBootHttpRequestDispatcher implements ITioHttpRequestHandler {

  protected HttpConfig httpConfig;
  protected TioBootHttpControllerRouter httpControllerRouter = null;
  private HttpRequestRouter httpRequestRouter;
  private HttpReqeustGroovyRouter httpGroovyRouter;
  private HttpRequestFunctionRouter httpRequestFunctionRouter;
  private HttpRequestInterceptor httpRequestInterceptor;
  private HttpSessionListener httpSessionListener;
  private ThrowableHandler throwableHandler;
  private SessionCookieDecorator sessionCookieDecorator;
  private IpPathAccessStats ipPathAccessStats;
  private TokenPathAccessStats tokenPathAccessStats;
  private RequestStatisticsHandler requestStatisticsHandler;
  private ResponseStatisticsHandler responseStatisticsHandler;
  private StaticResourceHandler staticResourceHandler;
  private HttpRequestHandler forwardHandler;
  private HttpRequestHandler notFoundHandler;
  private DynamicRequestController dynamicRequestController;
  private HttpRequestFunctionHandler httpRequestFunctionHandler;
  private AccessStatisticsHandler accessStatisticsHandler = new AccessStatisticsHandler();

  private boolean printReport = EnvUtils.getBoolean(ServerConfigKeys.SERVER_HTTP_REQUEST_PRINTREPORT, false);
  private boolean corsEnable = EnvUtils.getBoolean(ServerConfigKeys.SERVER_HTTP_RESPONSE_CORS_ENABLE, false);
  private boolean printUrl = EnvUtils.getBoolean(ServerConfigKeys.SERVER_HTTP_REQUEST_PRINT_URL, false);
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

  public void init(HttpConfig httpConfig, CacheFactory cacheFactory,
      //
      HttpRequestInterceptor defaultHttpServerInterceptorDispather,
      //
      HttpRequestRouter httpReqeustSimpleHandlerRoute, HttpReqeustGroovyRouter httpReqeustGroovyRoute,
      //
      HttpRequestFunctionRouter httpRequestFunctionRouter,
      //
      TioBootHttpControllerRouter tioBootHttpControllerRoutes,
      //
      HttpRequestHandler forwardHandler, HttpRequestHandler notFoundHandler,
      //
      RequestStatisticsHandler requestStatisticsHandler, ResponseStatisticsHandler responseStatisticsHandler) {

    this.httpControllerRouter = tioBootHttpControllerRoutes;
    this.httpRequestInterceptor = defaultHttpServerInterceptorDispather;
    this.httpRequestRouter = httpReqeustSimpleHandlerRoute;
    this.httpGroovyRouter = httpReqeustGroovyRoute;
    this.forwardHandler = forwardHandler;
    this.notFoundHandler = notFoundHandler;
    this.requestStatisticsHandler = requestStatisticsHandler;
    this.responseStatisticsHandler = responseStatisticsHandler;
    this.httpRequestFunctionRouter = httpRequestFunctionRouter;

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
    dynamicRequestController = new DynamicRequestController();
    httpRequestFunctionHandler = new HttpRequestFunctionHandler();
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
      String remark = "Incorrect domain name" + request.getDomain();
      Tio.remove(request.channelContext, remark);
      HttpResponse httpResponse = new HttpResponse(request).setString(remark);
      httpResponse.setKeepedConnectin(false);
      return httpResponse;
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
    if (printUrl) {
      log.info("access:{}", requestLine.toString());
    }

    // limit
    if (httpConfig.isUseSession() && EnvUtils.getBoolean("server.rate.limit.enable", true)) {
      httpResponse = SessionLimit.check(request, path, httpConfig, sessionRateLimiterCache);
      if (httpResponse != null) {
        return httpResponse;
      }
    }

    // options
    if (requestLine.method.equals(HttpMethod.OPTIONS)) { // allow all OPTIONS request
      httpResponse = new HttpResponse(request);
      CORSUtils.enableCORS(httpResponse, new HttpCors());
      return httpResponse;
    }

    // request
    if (requestStatisticsHandler != null) {
      requestStatisticsHandler.count(request);
    }

    requestLine = request.getRequestLine();
    path = requestLine.path;
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
                .append("interceptor:" + httpRequestInterceptor).append("\n")//
                .append("response:" + httpResponse).append("\n")//
                .append("\n");

            log.info(stringBuffer.toString());

          }
        }
      }

      HttpRequestHandler httpRequestHandler = null;
      if (httpResponse == null) {
        // simpleHandlerRoute
        httpRequestHandler = httpRequestRouter.find(path);
        if (httpRequestHandler != null) {
          if (printReport) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("\n-----------httpRequestRouter report---------------------\n");
            stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                .append("handler:" + httpRequestHandler.toString()).append("\n");
            log.info(stringBuffer.toString());
          }
          httpResponse = httpRequestHandler.handle(request);
        }

      }

      if (httpResponse == null) {
        // groovyRoutes
        if (httpGroovyRouter != null) {
          httpRequestHandler = httpGroovyRouter.find(path);
          if (httpRequestHandler != null) {
            if (printReport) {
              StringBuffer stringBuffer = new StringBuffer();
              stringBuffer.append("\n-----------httpGroovyRoutes report---------------------\n");
              stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                  .append("handler:" + httpRequestHandler.toString()).append("\n");
              log.info(stringBuffer.toString());
            }
            httpResponse = httpRequestHandler.handle(request);
          }
        }
      }

      if (httpResponse == null) {
        // httpRequestFunctionRouter
        if (httpRequestFunctionRouter != null) {
          RouteEntry<Object, Object> functionEntry = httpRequestFunctionRouter.find(path);
          if (functionEntry != null) {
            if (printReport) {
              StringBuffer stringBuffer = new StringBuffer();
              stringBuffer.append("\n-----------httpRequestFunctionRouter report---------------------\n");
              stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                  .append("handler:" + httpRequestHandler.toString()).append("\n");
              log.info(stringBuffer.toString());
            }
            httpResponse = httpRequestFunctionHandler.handleFunction(request, httpConfig, compatibilityAssignment, functionEntry, path);
          }
        }
      }

      // 执行动态请求
      if (httpResponse == null) {
        Method method = TioHttpControllerUtils.getActionMethod(request, requestLine, httpConfig, httpControllerRouter);
        if (method != null) {
          if (httpResponse == null) {
            if (printReport) {
              if (log.isInfoEnabled()) {
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("\n-----------action report---------------------\n");
                stringBuffer.append("request:" + requestLine.toString()).append("\n")//
                    .append("method:" + method.toString()).append("\n");
                log.info(stringBuffer.toString());
              }
            }
            httpResponse = dynamicRequestController.process(request, httpConfig, compatibilityAssignment, httpControllerRouter, method);
          }
        } else {
          // 转发请求
          if (forwardHandler != null) {
            httpResponse = forwardHandler.handle(request);
            if (httpResponse.getStatus().status == 404) {
              httpResponse = null;
            }
          }

          // 请求静态文件
          if (httpResponse == null && staticResourceHandler != null) {
            httpResponse = staticResourceHandler.processStatic(path, request);
          }
          // 404
          if (httpResponse == null) {
            httpResponse = resp404(request, requestLine);
          }
        }

      }

      if (corsEnable) {
        CORSUtils.enableCORS(httpResponse);
      }
      return httpResponse;

    } catch (Throwable e) {
      httpResponse = resp500(request, requestLine, e);
      if (corsEnable) {
        CORSUtils.enableCORS(httpResponse);
      }
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
    String domain = TioHttpControllerUtils.getDomain(request);
    String name = httpConfig.getSessionCookieName();
    long maxAge = Math.max(httpConfig.getSessionTimeout() * 30, 3600 * 24 * 365 * 10);

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
    if (httpControllerRouter != null) {
      String page404 = httpConfig.getPage404();
      if (page404 != null) {
        Method method = httpControllerRouter.PATH_METHOD_MAP.get(page404);
        if (method != null) {
          return Resps.forward(request, page404);
        }
      }

    }

    return Resps.resp404(request, requestLine, httpConfig);
  }

  @Override
  public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append(SysConst.CRLF).append("remote  :").append(request.getClientIp());
    sb.append(SysConst.CRLF).append("request :").append(requestLine.toString());
    log.error(sb.toString(), throwable);

    if (throwableHandler != null) {
      return throwableHandler.handler(request, requestLine, throwable);
    }

    String page500 = httpConfig.getPage500();
    TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();

    if (page500 != null) {
      if (exceptionHandler != null) {
        exceptionHandler.handler(request, throwable);
      }
      if (httpControllerRouter != null) {
        Method method = httpControllerRouter.PATH_METHOD_MAP.get(page500);
        if (method != null) {
          return Resps.forward(request, page500);
        }
      }
    } else if (exceptionHandler != null) {
      Object result = exceptionHandler.handler(request, throwable);
      if (result != null) {
        HttpResponse response = TioRequestContext.getResponse();
        return response.setJson(result);
      }
    }

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
  public void setStaticResCache(AbsCache staticResCache) {
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