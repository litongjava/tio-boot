package com.litongjava.tio.boot.http.handler;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.tio.boot.constatns.TioBootConfigKeys;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.TioControllerContext;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpServerInterceptorDispatcher;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoutes;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResource;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.common.view.freemarker.FreemarkerConfig;
import com.litongjava.tio.http.server.annotation.EnableCORS;
import com.litongjava.tio.http.server.handler.FileCache;
import com.litongjava.tio.http.server.handler.HttpRequestRouteHandler;
import com.litongjava.tio.http.server.intf.CurrUseridGetter;
import com.litongjava.tio.http.server.intf.HttpServerInterceptor;
import com.litongjava.tio.http.server.intf.HttpSessionListener;
import com.litongjava.tio.http.server.intf.ThrowableHandler;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.router.DbHttpRoutes;
import com.litongjava.tio.http.server.router.HttpRoutes;
import com.litongjava.tio.http.server.session.HttpSessionUtils;
import com.litongjava.tio.http.server.session.SessionCookieDecorator;
import com.litongjava.tio.http.server.stat.StatPathFilter;
import com.litongjava.tio.http.server.stat.ip.path.IpAccessStat;
import com.litongjava.tio.http.server.stat.ip.path.IpPathAccessStat;
import com.litongjava.tio.http.server.stat.ip.path.IpPathAccessStatListener;
import com.litongjava.tio.http.server.stat.ip.path.IpPathAccessStats;
import com.litongjava.tio.http.server.stat.token.TokenAccessStat;
import com.litongjava.tio.http.server.stat.token.TokenPathAccessStat;
import com.litongjava.tio.http.server.stat.token.TokenPathAccessStatListener;
import com.litongjava.tio.http.server.stat.token.TokenPathAccessStats;
import com.litongjava.tio.http.server.util.HttpServerResponseUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.IoUtils;
import com.litongjava.tio.utils.SysConst;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCache;
import com.litongjava.tio.utils.environment.EnvironmentUtils;
import com.litongjava.tio.utils.freemarker.FreemarkerUtils;
import com.litongjava.tio.utils.hutool.ArrayUtil;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.StrUtil;

import freemarker.template.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * @author litongjava
 */
@Slf4j
public class DefaultHttpRequestHandlerDispather implements HttpRequestHandler {

  private static final Map<Class<?>, MethodAccess> CLASS_METHODACCESS_MAP = new HashMap<>();
  protected HttpConfig httpConfig;
  protected TioBootHttpControllerRoutes routes = null;
  private HttpRoutes httpRoutes;
  private DbHttpRoutes dbRoutes;
  private HttpServerInterceptor httpServerInterceptor;
  private HttpSessionListener httpSessionListener;
  private ThrowableHandler throwableHandler;
  private SessionCookieDecorator sessionCookieDecorator;
  private IpPathAccessStats ipPathAccessStats;
  private TokenPathAccessStats tokenPathAccessStats;
  private HandlerDispatcher handlerDispather = new HandlerDispatcher();
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
  private CacheFactory cacheFactory;

  /**
   * 设置配置,路由和拦截器
   *
   * @param httpConfig                   http config
   * @param routes                       routes
   * @param defaultHttpServerInterceptor interceptor
   * @param httpRoutes                   http route
   * @param cacheFactory                 cacheFactory
   * @throws Exception
   */
  public DefaultHttpRequestHandlerDispather(HttpConfig httpConfig, TioBootHttpControllerRoutes routes,
      DefaultHttpServerInterceptorDispatcher defaultHttpServerInterceptor, HttpRoutes httpRoutes, DbHttpRoutes dbRoutes,
      CacheFactory cacheFactory) throws Exception {

    this.httpServerInterceptor = defaultHttpServerInterceptor;
    this.httpRoutes = httpRoutes;
    this.dbRoutes = dbRoutes;
    this.cacheFactory = cacheFactory;
    this.routes = routes;
    init(httpConfig);

  }

  private void init(HttpConfig httpConfig) throws Exception {
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
      // staticResCache = CaffeineCache.register(STATIC_RES_CONTENT_CACHENAME,maxLiveTimeOfStaticRes, null);
      staticResCache = cacheFactory.register(DefaultHttpRequestConstants.STATIC_RES_CONTENT_CACHENAME,
          maxLiveTimeOfStaticRes, null);

    }

    sessionRateLimiterCache = cacheFactory.register(DefaultHttpRequestConstants.SESSION_RATE_LIMITER_CACHENAME, 60 * 1L,
        null);

    this.monitorFileChanged();
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

  public HttpServerInterceptor getHttpServerInterceptor() {
    return httpServerInterceptor;
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

    // 检查域名
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
      } else {

      }
    }

    if (StrUtil.isNotBlank(suffix)) {
      if (StrUtil.endWith(path, suffix)) {
        path = StrUtil.sub(path, 0, path.length() - suffixLength);
      } else {

      }
    }
    requestLine.setPath(path);

    HttpResponse httpResponse = null;
    try {
      TioControllerContext.hold(request);
      processCookieBeforeHandler(request, requestLine);

      path = requestLine.path;

      // 流控
      if (httpConfig.isUseSession()) {
        httpResponse = SessionLimit.build(request, path, httpConfig, sessionRateLimiterCache);
        if (httpResponse != null) {
          return httpResponse;
        }
      }

      String httpMethod = request.getMethod();
      if ("OPTIONS".equals(httpMethod)) { // allow all OPTIONS request
        httpResponse = new HttpResponse(request);
        HttpServerResponseUtils.enableCORS(httpResponse, new HttpCors());
        return httpResponse;
      }

      // Interceptor
      requestLine = request.getRequestLine();
      boolean printReport = EnvironmentUtils.getBoolean("tio.mvc.request.printReport", false);
      if (httpServerInterceptor != null) {
        httpResponse = httpServerInterceptor.doBeforeHandler(request, requestLine, httpResponse);
        if (httpResponse != null) {
          if (printReport) {
            if (log.isInfoEnabled()) {
              log.info("-----------action report---------------------");
              log.info("request:{}", requestLine);
              log.info("httpServerInterceptor:{}", httpServerInterceptor);
              log.info("response:{}", httpResponse);
              log.info("---------------------------------------------");
            }
          }
          return httpResponse;
        }
      }

      // 查找simpleRoute
      if (httpRoutes != null) {
        HttpRequestRouteHandler httpRequestRouteHandler = httpRoutes.find(path);
        if (httpRequestRouteHandler != null) {
          if (printReport) {
            printRequestLog(requestLine, httpRequestRouteHandler);
          }
          httpResponse = httpRequestRouteHandler.handle(request);
        }
      }

      // 查找DbRoute
      if (dbRoutes != null) {
        HttpRequestRouteHandler httpRequestRouteHandler = dbRoutes.find(path);
        if (httpRequestRouteHandler != null) {
          if (printReport) {
            printRequestLog(requestLine, httpRequestRouteHandler);
          }
          httpResponse = httpRequestRouteHandler.handle(request);
        }
      }

      path = requestLine.path;

      Method method = TioHttpHandlerUtil.getActionMethod(httpConfig, routes, request, requestLine);
      // 执行动态请求
      if (httpResponse == null && method != null) {
        if (printReport) {
          if (log.isInfoEnabled()) {
            log.info("-----------action report---------------------");
            System.out.println("request:" + requestLine.toString());
            System.out.println("method:" + method.toString());
            log.info("---------------------------------------------");
          }
        }
        httpResponse = this.processDynamic(httpConfig, routes, compatibilityAssignment, CLASS_METHODACCESS_MAP, request,
            method);
      }

      // 请求静态文件
      if (httpResponse == null && method == null) {
        httpResponse = this.processStatic(path, request);
      }

      if (httpResponse == null && method == null) {
        httpResponse = resp404(request, requestLine);// Resps.html(request, "404--并没有找到你想要的内容", httpConfig.getCharset());
      }

      return httpResponse;
    } catch (Throwable e) {
      logError(request, requestLine, e);
      return resp500(request, requestLine, e);// Resps.html(request, "500--服务器出了点故障", httpConfig.getCharset());
    } finally {
      TioControllerContext.release();
      // print url
      if (EnvironmentUtils.getBoolean(TioBootConfigKeys.TIO_HTTP_REQUEST_PRINT_URL)) {
        System.out.println(path);
      }
      try {
        long time = SystemTimer.currTime;
        long iv = time - start; // 本次请求消耗的时间，单位：毫秒
        try {
          processCookieAfterHandler(request, requestLine, httpResponse);
        } catch (Throwable e) {
          logError(request, requestLine, e);
        } finally {
          if (httpServerInterceptor != null) {
            try {
              httpServerInterceptor.doAfterHandler(request, requestLine, httpResponse, iv);
            } catch (Exception e) {
              log.error(requestLine.toString(), e);
            }
          }

          boolean f = statIpPath(request, httpResponse, path, iv);
          if (!f) {
            return null;
          }

          f = statTokenPath(request, httpResponse, path, iv);
          if (!f) {
            return null;
          }
        }
      } catch (Exception e) {
        log.error(request.requestLine.toString(), e);
      } finally {
        if (request.isNeedForward()) {
          request.setForward(true);
          return handler(request);
        }
      }
    }
  }

  /**
   * 打印请求日志
   */
  private void printRequestLog(RequestLine requestLine, HttpRequestRouteHandler httpRequestRouteHandler) {
    if (log.isInfoEnabled()) {
      log.info("-----------action report---------------------");
      System.out.println("request:" + requestLine.toString());
      System.out.println("handler:" + httpRequestRouteHandler.toString());
      log.info("---------------------------------------------");
    }
  }

  private HttpResponse processDynamic(HttpConfig httpConfig, TioBootHttpControllerRoutes routes, boolean compatibilityAssignment,
      Map<Class<?>, MethodAccess> classMethodaccessMap, HttpRequest request, Method actionMethod) {
    // execute
    HttpResponse response = handlerDispather.executeAction(httpConfig, routes, compatibilityAssignment,
        CLASS_METHODACCESS_MAP, request, actionMethod);

    boolean isEnableCORS = false;
    EnableCORS enableCORS = actionMethod.getAnnotation(EnableCORS.class);
    if (enableCORS != null) {
      isEnableCORS = true;
      HttpServerResponseUtils.enableCORS(response, new HttpCors(enableCORS));
    }

    if (isEnableCORS == false) {
      Object controllerBean = routes.METHOD_BEAN_MAP.get(actionMethod);
      EnableCORS controllerEnableCORS = controllerBean.getClass().getAnnotation(EnableCORS.class);

      if (controllerEnableCORS != null) {
        isEnableCORS = true;
        HttpServerResponseUtils.enableCORS(response, new HttpCors(controllerEnableCORS));
      }
    }
    return response;
  }

  private HttpResponse processStatic(String path, HttpRequest request) throws Exception {
    HttpResponse response = null;
    FileCache fileCache = null;
    File file = null;
    if (staticResCache != null) {
      // contentCache = CaffeineCache.getCache(STATIC_RES_CONTENT_CACHENAME);
      fileCache = (FileCache) staticResCache.get(path);
    }
    if (fileCache != null) {
      // byte[] bodyBytes = fileCache.getData();
      // Map<String, String> headers = fileCache.getHeaders();

      // HttpResponse responseInCache = fileCache.getResponse();

      long lastModified = fileCache.getLastModified();

      response = Resps.try304(request, lastModified);
      if (response != null) {
        response.addHeader(HeaderName.tio_from_cache, HeaderValue.Tio_From_Cache.TRUE);
        return response;
      }

      // response = fileCache.cloneResponse(request);
      response = fileCache.getResponse();
      response = HttpResponse.cloneResponse(request, response);

      // log.info("{}, 从缓存获取, 大小: {}", path, response.getBody().length);

      // response = new HttpResponse(request, httpConfig);
      // response.setBody(bodyBytes, request);
      // response.addHeaders(headers);
      return response;
    } else {
      String pageRoot = httpConfig.getPageRoot(request);
      if (pageRoot != null) {
        HttpResource httpResource = httpConfig.getResource(request, path);// .getFile(request, path);
        if (httpResource != null) {
          path = httpResource.getPath();
          file = httpResource.getFile();
          String template = httpResource.getPath(); // "/res/css/header-all.css"
          InputStream inputStream = httpResource.getInputStream();

          String extension = FileUtil.extName(template);

          // 项目中需要，时间支持一下freemarker模板，后面要做模板支持抽象设计
          FreemarkerConfig freemarkerConfig = httpConfig.getFreemarkerConfig();
          if (freemarkerConfig != null) {
            if (ArrayUtil.contains(freemarkerConfig.getSuffixes(), extension)) {
              Configuration configuration = freemarkerConfig.getConfiguration(request);
              if (configuration != null) {
                Object model = freemarkerConfig.getModelGenerator().generate(request);
                if (request.isClosed()) {
                  return null;
                } else {
                  if (model instanceof HttpResponse) {
                    response = (HttpResponse) model;
                    return response;
                  } else {
                    try {
                      String retStr = FreemarkerUtils.generateStringByPath(template, configuration, model);
                      response = Resps.bytes(request, retStr.getBytes(configuration.getDefaultEncoding()), extension);
                      return response;
                    } catch (Throwable e) {
                      log.error("freemarker错误，当成普通文本处理：" + file.getCanonicalPath() + ", " + e.toString());
                    }
                  }
                }
              }
            }
          }

          if (file != null) {
            response = Resps.file(request, file);
          } else {
            response = Resps.bytes(request, IoUtils.toByteArray(inputStream), extension);// .file(request, file);
          }

          response.setStaticRes(true);

          // 把静态资源放入缓存
          if (response.isStaticRes() && staticResCache != null/** && request.getIsSupportGzip()*/
          ) {
            if (response.getBody() != null && response.getStatus() == HttpResponseStatus.C200) {
              HeaderValue contentType = response.getHeader(HeaderName.Content_Type);
              HeaderValue contentEncoding = response.getHeader(HeaderName.Content_Encoding);
              HeaderValue lastModified = response.getLastModified();// .getHeader(HttpConst.ResponseHeaderKey.Last_Modified);

              Map<HeaderName, HeaderValue> headers = new HashMap<>();
              if (contentType != null) {
                headers.put(HeaderName.Content_Type, contentType);
              }
              if (contentEncoding != null) {
                headers.put(HeaderName.Content_Encoding, contentEncoding);
              }

              HttpResponse responseInCache = new HttpResponse(request);
              responseInCache.addHeaders(headers);
              if (lastModified != null) {
                responseInCache.setLastModified(lastModified);
              }
              responseInCache.setBody(response.getBody());
              responseInCache.setHasGzipped(response.isHasGzipped());

              if (file != null) {
                fileCache = new FileCache(responseInCache, file.lastModified());
              } else {
                fileCache = new FileCache(responseInCache, ManagementFactory.getRuntimeMXBean().getStartTime());
              }

              staticResCache.put(path, fileCache);
              if (log.isInfoEnabled()) {
                log.info("add to cache:[{}], {}(B)", path, response.getBody().length);
              }
            }
          }
          return response;
        }
      }
    }
    return response;
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

  /**
   * ipPathAccessStat and ipAccessStat
   *
   * @param request
   * @param response
   * @param path
   * @param iv
   * @return
   */
  private boolean statIpPath(HttpRequest request, HttpResponse response, String path, long iv) {
    if (ipPathAccessStats == null) {
      return true;
    }

    if (response == null) {
      return false;
    }

    if (response.isSkipIpStat() || request.isClosed()) {
      return true;
    }

    // 统计一下IP访问数据
    String ip = request.getClientIp();// IpUtils.getRealIp(request);

    // Cookie cookie = getSessionCookie(request, httpConfig);
    String sessionId = HttpSessionUtils.getSessionId(request);

    StatPathFilter statPathFilter = ipPathAccessStats.getStatPathFilter();

    // 添加统计
    for (Long duration : ipPathAccessStats.durationList) {
      IpAccessStat ipAccessStat = ipPathAccessStats.get(duration, ip);// .get(duration, ip, path);//.get(v, channelContext.getClientNode().getIp());

      ipAccessStat.count.incrementAndGet();
      ipAccessStat.timeCost.addAndGet(iv);
      ipAccessStat.setLastAccessTime(SystemTimer.currTime);
      if (StrUtil.isBlank(sessionId)) {
        ipAccessStat.noSessionCount.incrementAndGet();
      } else {
        ipAccessStat.sessionIds.add(sessionId);
      }

      if (statPathFilter.filter(path, request, response)) {
        IpPathAccessStat ipPathAccessStat = ipAccessStat.get(path);
        ipPathAccessStat.count.incrementAndGet();
        ipPathAccessStat.timeCost.addAndGet(iv);
        ipPathAccessStat.setLastAccessTime(SystemTimer.currTime);

        if (StrUtil.isBlank(sessionId)) {
          ipPathAccessStat.noSessionCount.incrementAndGet();
        }
        // else {
        // ipAccessStat.sessionIds.add(cookie.getValue());
        // }

        IpPathAccessStatListener ipPathAccessStatListener = ipPathAccessStats.getListener(duration);
        if (ipPathAccessStatListener != null) {
          boolean isContinue = ipPathAccessStatListener.onChanged(request, ip, path, ipAccessStat, ipPathAccessStat);
          if (!isContinue) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * tokenPathAccessStat
   *
   * @param request
   * @param response
   * @param path
   * @param iv
   * @return
   */
  private boolean statTokenPath(HttpRequest request, HttpResponse response, String path, long iv) {
    if (tokenPathAccessStats == null) {
      return true;
    }

    if (response == null) {
      return false;
    }

    if (response.isSkipTokenStat() || request.isClosed()) {
      return true;
    }

    // 统计一下Token访问数据
    String token = tokenPathAccessStats.getTokenGetter().getToken(request);
    if (StrUtil.isNotBlank(token)) {
      List<Long> list = tokenPathAccessStats.durationList;

      CurrUseridGetter currUseridGetter = tokenPathAccessStats.getCurrUseridGetter();
      String uid = null;
      if (currUseridGetter != null) {
        uid = currUseridGetter.getUserid(request);
      }

      StatPathFilter statPathFilter = tokenPathAccessStats.getStatPathFilter();

      // 添加统计
      for (Long duration : list) {
        TokenAccessStat tokenAccessStat = tokenPathAccessStats.get(duration, token, request.getClientIp(), uid);// .get(duration, ip, path);//.get(v, channelContext.getClientNode().getIp());

        tokenAccessStat.count.incrementAndGet();
        tokenAccessStat.timeCost.addAndGet(iv);
        tokenAccessStat.setLastAccessTime(SystemTimer.currTime);

        if (statPathFilter.filter(path, request, response)) {
          TokenPathAccessStat tokenPathAccessStat = tokenAccessStat.get(path);
          tokenPathAccessStat.count.incrementAndGet();
          tokenPathAccessStat.timeCost.addAndGet(iv);
          tokenPathAccessStat.setLastAccessTime(SystemTimer.currTime);

          TokenPathAccessStatListener tokenPathAccessStatListener = tokenPathAccessStats.getListener(duration);
          if (tokenPathAccessStatListener != null) {
            boolean isContinue = tokenPathAccessStatListener.onChanged(request, token, path, tokenAccessStat,
                tokenPathAccessStat);
            if (!isContinue) {
              return false;
            }
          }
        }
      }
    }

    return true;
  }

  private void logError(HttpRequest request, RequestLine requestLine, Throwable e) {
    TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();
    if (exceptionHandler != null) {
      exceptionHandler.handler(request, e);
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(SysConst.CRLF).append("remote  :").append(request.getClientIp());
      sb.append(SysConst.CRLF).append("request :").append(requestLine.toString());
      log.error(sb.toString(), e);
      log.error(sb.toString(), e);
    }
  }

  private void processCookieAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse)
      throws ExecutionException {
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
      HttpSession httpSession1 = (HttpSession) httpConfig.getSessionStore().get(sessionId);

      if (httpSession1 == null) {// 有cookie但是超时了
        createSessionCookie(request, httpSession, httpResponse, false);
      }
    }
  }

  /**
   * 根据session创建session对应的cookie
   * 注意：先有session，后有session对应的cookie
   *
   * @param request
   * @param httpSession
   * @param httpResponse
   * @param forceCreate
   * @return
   */
  private void createSessionCookie(HttpRequest request, HttpSession httpSession, HttpResponse httpResponse,
      boolean forceCreate) {
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
          log.info("{} session【{}】 timeout", request.channelContext, sessionId);
        }

        httpSession = createSession(request);
      }
    }
    request.setHttpSession(httpSession);
  }

  @Override
  public HttpResponse resp404(HttpRequest request, RequestLine requestLine) throws Exception {
    if (routes != null) {
      String page404 = httpConfig.getPage404();
      Method method = routes.PATH_METHOD_MAP.get(page404);
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

    if (routes != null) {
      String page500 = httpConfig.getPage500();
      Method method = routes.PATH_METHOD_MAP.get(page500);
      if (method != null) {
        return Resps.forward(request, page500);
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

  public void setHttpServerInterceptor(HttpServerInterceptor httpServerInterceptor) {
    this.httpServerInterceptor = httpServerInterceptor;
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
