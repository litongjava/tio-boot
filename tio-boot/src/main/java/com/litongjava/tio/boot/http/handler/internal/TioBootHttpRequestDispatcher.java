package com.litongjava.tio.boot.http.handler.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.hook.HookCan;
import com.litongjava.model.sys.SysConst;
import com.litongjava.tio.boot.cache.StaticResourcesCache;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.http.handler.controller.DynamicRequestController;
import com.litongjava.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.http.session.SessionLimit;
import com.litongjava.tio.boot.http.utils.TioHttpControllerUtils;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.watch.DirectoryWatcher;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.exception.TioHandlePacketException;
import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpMethod;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.common.utils.HttpIpUtils;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.handler.RouteEntry;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.intf.HttpSessionListener;
import com.litongjava.tio.http.server.intf.ThrowableHandler;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.router.HttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpRequestGroovyRouter;
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

/**
 * Handles HTTP requests by routing them to appropriate handlers, managing
 * sessions, enforcing rate limits, and handling exceptions.
 * 
 * @author Tong Li
 */
public class TioBootHttpRequestDispatcher implements ITioHttpRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(TioBootHttpRequestDispatcher.class);
  
  protected HttpConfig httpConfig;
  protected TioBootHttpControllerRouter httpControllerRouter = null;
  private HttpRequestRouter httpRequestRouter;
  private HttpRequestGroovyRouter httpGroovyRouter;
  private HttpRequestFunctionRouter httpRequestFunctionRouter;
  private HttpRequestInterceptor httpRequestInterceptor;
  private HttpRequestInterceptor httpRequestValidationInterceptor;
  private HttpRequestInterceptor authTokenInterceptor;
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
   * Rate limiter cache.
   */
  private AbsCache sessionRateLimiterCache;

  private String contextPath;
  private int contextPathLength = 0;
  private String suffix;
  private int suffixLength = 0;

  /**
   * Compatibility flag for assignment.
   */
  private boolean compatibilityAssignment = true;

  /**
   * Initializes the HTTP request dispatcher with necessary configurations and
   * handlers.
   * 
   * @param httpConfig                    The HTTP configuration.
   * @param cacheFactory                  The cache factory for managing caches.
   * @param defaultHttpRequestInterceptor The default HTTP request interceptor
   *                                      dispatcher.
   * @param httpRequestRouter             The simple HTTP request router.
   * @param httpRequestGroovyRouter       The Groovy-based HTTP request router.
   * @param httpRequestFunctionRouter     The function-based HTTP request router.
   * @param tioBootHttpControllerRoutes   The HTTP controller router.
   * @param forwardHandler                The handler for forwarding requests.
   * @param notFoundHandler               The handler for 404 responses.
   * @param requestStatisticsHandler      The handler for request statistics.
   * @param responseStatisticsHandler     The handler for response statistics.
   * @param staticResourceHandler         The handler for static resources.
   */
  public void init(HttpConfig httpConfig, CacheFactory cacheFactory,
      //
      HttpRequestInterceptor defaultHttpRequestInterceptor, HttpRequestInterceptor httpRequestValidationInterceptor,
      HttpRequestInterceptor authTokenInterceptor,
      //
      HttpRequestRouter httpRequestRouter, HttpRequestGroovyRouter httpRequestGroovyRouter,
      //
      HttpRequestFunctionRouter httpRequestFunctionRouter, TioBootHttpControllerRouter tioBootHttpControllerRoutes,
      //
      HttpRequestHandler forwardHandler, HttpRequestHandler notFoundHandler,
      //
      RequestStatisticsHandler requestStatisticsHandler, ResponseStatisticsHandler responseStatisticsHandler,
      //
      StaticResourceHandler staticResourceHandler) {

    this.httpControllerRouter = tioBootHttpControllerRoutes;
    this.httpRequestInterceptor = defaultHttpRequestInterceptor;
    this.httpRequestValidationInterceptor = httpRequestValidationInterceptor;
    this.authTokenInterceptor = authTokenInterceptor;
    this.httpRequestRouter = httpRequestRouter;
    this.httpGroovyRouter = httpRequestGroovyRouter;
    this.forwardHandler = forwardHandler;
    this.notFoundHandler = notFoundHandler;
    this.requestStatisticsHandler = requestStatisticsHandler;
    this.responseStatisticsHandler = responseStatisticsHandler;
    this.httpRequestFunctionRouter = httpRequestFunctionRouter;

    if (httpConfig == null) {
      throw new RuntimeException("httpConfig cannot be null");
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

    // Initialize static resource cache if caching is enabled
    if (httpConfig.getMaxLiveTimeOfStaticRes() > 0) {
      long maxLiveTimeOfStaticRes = (long) httpConfig.getMaxLiveTimeOfStaticRes();
      AbsCache staticResCache = cacheFactory.register(DefaultHttpRequestConstants.STATIC_RES_CONTENT_CACHE_NAME,
          maxLiveTimeOfStaticRes, null);
      StaticResourcesCache.setStaticResCache(staticResCache);
      StaticResourcesCache.setHttpConfig(httpConfig);
    }

    // Initialize session rate limiter cache
    sessionRateLimiterCache = cacheFactory.register(DefaultHttpRequestConstants.SESSION_RATE_LIMITER_CACHE_NAME,
        60 * 1L, null);

    // Monitor file changes for dynamic content
    if (httpConfig.getPageRoot() != null) {
      try {
        this.monitorFileChanges();
      } catch (Exception e) {
        log.error("Error setting up file change monitor", e);
      }
    }

    // Initialize static resource handler
    if (staticResourceHandler == null) {
      this.staticResourceHandler = new DefaultStaticResourceHandler();
    } else {
      this.staticResourceHandler = staticResourceHandler;
    }

    // Initialize dynamic request controller and function handler
    dynamicRequestController = new DynamicRequestController();
    httpRequestFunctionHandler = new HttpRequestFunctionHandler();
  }

  /**
   * Creates a new HTTP session.
   *
   * @param request The HTTP request.
   * @return The created HttpSession.
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
   * Retrieves the HTTP configuration for a given request.
   *
   * @param request The HTTP request.
   * @return The HttpConfig.
   */
  public HttpConfig getHttpConfig(HttpRequest request) {
    return httpConfig;
  }

  /**
   * Retrieves the HTTP server interceptor.
   *
   * @return The HttpRequestInterceptor.
   */
  public HttpRequestInterceptor getHttpServerInterceptor() {
    return httpRequestInterceptor;
  }

  /**
   * Checks if the domain of the request is allowed to access the server.
   *
   * @param request The HTTP request.
   * @return True if the domain is allowed, false otherwise.
   */
  private boolean checkDomain(HttpRequest request) {
    String[] allowDomains = httpConfig.getAllowDomains();
    if (allowDomains == null || allowDomains.length == 0) {
      return true;
    }
    String host = request.getHost();
    return ArrayUtil.contains(allowDomains, host);
  }

  /**
   * Handles the incoming HTTP request and returns the appropriate HTTP response.
   *
   * @param request The HTTP request.
   * @return The HTTP response.
   * @throws Exception If an error occurs during handling.
   */
  @Override
  public HttpResponse handler(HttpRequest request) throws Exception {

    request.setNeedForward(false);

    // Check if the domain is allowed
    if (!checkDomain(request)) {
      String remark = "Incorrect domain name: " + request.getDomain();
      Tio.remove(request.channelContext, remark);
      HttpResponse httpResponse = new HttpResponse(request).body(remark);
      httpResponse.setKeepConnection(false);
      return httpResponse;
    }

    long startTime = SystemTimer.currTime;

    RequestLine requestLine = request.getRequestLine();
    String path = requestLine.path;

    // Remove context path from the request path
    if (StrUtil.isNotBlank(contextPath) && StrUtil.startWith(path, contextPath)) {
      path = StrUtil.subSuf(path, contextPathLength);
    }

    // Remove suffix from the request path
    if (StrUtil.isNotBlank(suffix) && StrUtil.endWith(path, suffix)) {
      path = StrUtil.sub(path, 0, path.length() - suffixLength);
    }
    requestLine.setPath(path);

    // Log the request URL if enabled
    if (printUrl) {
      log.info("From: {} Accessed: {}",HttpIpUtils.getRealIp(request), requestLine.toString());
    }
    // Handle OPTIONS requests for CORS preflight
    if (HttpMethod.OPTIONS.equals(requestLine.method)) {
      HttpResponse httpResponse = new HttpResponse(request);
      CORSUtils.enableCORS(httpResponse, new HttpCors());
      return httpResponse;
    }

    // Process cookies before handling the request
    processCookieBeforeHandler(request, requestLine);

    // Enforce rate limiting if sessions are used and rate limiting is enabled
    if (httpConfig.isUseSession() && EnvUtils.getBoolean(ServerConfigKeys.SERVER_RATE_LIMIT_ENEABLE, true)) {
      HttpResponse httpResponse = SessionLimit.check(request, path, httpConfig, sessionRateLimiterCache);
      if (httpResponse != null) {
        return httpResponse;
      }
    }

    // Record request statistics
    if (requestStatisticsHandler != null) {
      requestStatisticsHandler.count(request);
    }

    requestLine = request.getRequestLine();
    path = requestLine.path;

    HttpResponse httpResponse = new HttpResponse(request);
    try {
      TioRequestContext.hold(request, httpResponse);

      if (httpRequestValidationInterceptor != null) {
        httpResponse = httpRequestValidationInterceptor.doBeforeHandler(request, requestLine, httpResponse);
        if (httpResponse != null) {
          return httpResponse;
        }
      }

      if (authTokenInterceptor != null) {
        httpResponse = authTokenInterceptor.doBeforeHandler(request, requestLine, httpResponse);
        if (httpResponse != null) {
          return httpResponse;
        }
      }

      // Execute before-handler interceptors
      httpResponse = httpRequestInterceptor.doBeforeHandler(request, requestLine, httpResponse);
      if (httpResponse != null && printReport) {
        logInterceptorReport(requestLine, httpResponse);
      }

      HttpRequestHandler httpRequestHandler = null;

      // Route to simple handler if no response yet
      if (httpResponse == null) {
        httpRequestHandler = httpRequestRouter.resolve(request);
        if (httpRequestHandler != null) {
          if (printReport) {
            logRouterReport(requestLine, httpRequestHandler, "httpRequestRouter");
          }
          httpResponse = httpRequestHandler.handle(request);
        }
      }

      // Route to Groovy handler if no response yet
      if (httpResponse == null && httpGroovyRouter != null) {
        httpRequestHandler = httpGroovyRouter.find(path);
        if (httpRequestHandler != null) {
          if (printReport) {
            logRouterReport(requestLine, httpRequestHandler, "httpGroovyRouter");
          }
          httpResponse = httpRequestHandler.handle(request);
        }
      }

      // Route to function handler if no response yet
      if (httpResponse == null && httpRequestFunctionRouter != null) {
        RouteEntry<Object, Object> functionEntry = httpRequestFunctionRouter.find(path);
        if (functionEntry != null) {
          if (printReport) {
            logFunctionRouterReport(requestLine, functionEntry);
          }
          httpResponse = httpRequestFunctionHandler.handleFunction(request, httpConfig, compatibilityAssignment,
              functionEntry, path);
        }
      }

      // Execute dynamic request controller if no response yet
      if (httpResponse == null) {
        Method method = TioHttpControllerUtils.getActionMethod(request, requestLine, httpConfig, httpControllerRouter);
        if (method != null) {
          if (printReport) {
            logActionReport(requestLine, method);
          }
          httpResponse = dynamicRequestController.process(request, httpConfig, compatibilityAssignment,
              httpControllerRouter, method);
        } else {
          // Forward request if no handler found
          if (forwardHandler != null) {
            httpResponse = forwardHandler.handle(request);
            if (httpResponse.getStatus().status == 404) {
              httpResponse = null;
            }
          }

          // Handle static resources if no response yet
          if (httpResponse == null && staticResourceHandler != null) {
            httpResponse = staticResourceHandler.handle(path, request, httpConfig,
                StaticResourcesCache.getStaticResCache());
          }

          // Respond with 404 if still no response
          if (httpResponse == null) {
            httpResponse = resp404(request, requestLine);
          }
        }
      }

      // Enable CORS if configured
      if (corsEnable) {
        CORSUtils.enableCORS(httpResponse);
      }
      return httpResponse;

    } catch (Throwable e) {
      try {
        httpResponse = resp500(request, requestLine, e);
        if (corsEnable) {
          CORSUtils.enableCORS(httpResponse);
        }
      } catch (Exception e1) {
        throw new TioHandlePacketException(e1);
      }

      return httpResponse;
    } finally {
      Object userId = TioRequestContext.getUserId();

      TioRequestContext.release();
      long endTime = SystemTimer.currTime;
      long elapsedTime = endTime - startTime; // Time taken for this request in milliseconds

      try {
        processCookieAfterHandler(request, requestLine, httpResponse);
      } catch (Exception e) {
        log.error("Error processing cookies after handler for request: {}", requestLine, e);
      }

      // Execute after-handler interceptors
      if (httpRequestInterceptor != null) {
        try {
          httpRequestInterceptor.doAfterHandler(request, requestLine, httpResponse, elapsedTime);
        } catch (Exception e) {
          log.error("Error executing after handler interceptor for request: {}", requestLine, e);
        }
      }

      // Update access statistics
      if (ipPathAccessStats != null) {
        accessStatisticsHandler.statIpPath(ipPathAccessStats, request, httpResponse, path, elapsedTime);
      }

      if (tokenPathAccessStats != null) {
        accessStatisticsHandler.statTokenPath(tokenPathAccessStats, request, httpResponse, path, elapsedTime);
      }

      // Handle request forwarding if needed
      if (request.isNeedForward()) {
        request.setForward(true);
        return handler(request);
      } else {
        if (responseStatisticsHandler != null) {
          try {
            this.responseStatisticsHandler.count(request, requestLine, httpResponse, userId, elapsedTime);
          } catch (Exception e) {
            log.error("Error counting response statistics for request: {}", requestLine, e);
          }
        }
      }
    }

  }

  /**
   * Monitors file changes in the page root directory.
   *
   * @throws Exception If an error occurs while setting up the file monitor.
   */
  public void monitorFileChanges() {
    String pageRoot = httpConfig.getPageRoot();
    if (httpConfig.isMonitorFileChange() && pageRoot != null) {
      try {
        Path pageRootPath = Paths.get(pageRoot);
        if (Files.exists(pageRootPath) && Files.isDirectory(pageRootPath)) {
          DirectoryWatcher directoryWatcher = new DirectoryWatcher(pageRootPath);
          directoryWatcher.start();
          TioBootServer.me().setStaticResourcesDirectoryWatcher(directoryWatcher);
          HookCan.me().addDestroyMethod(() -> {
            DirectoryWatcher staticDirectoryWatcher = TioBootServer.me().getStaticResourcesDirectoryWatcher();
            if (staticDirectoryWatcher != null) {
              directoryWatcher.stop();
            }
          });
          log.info("Started JDK WatchService monitor for directory: {}", pageRootPath);
        } else {
          log.warn("No such directory: {}", pageRootPath);
        }

      } catch (IOException e) {
        log.error("Error setting up WatchService for pageRoot", e);
      }
    }
  }

  /**
   * Processes cookies after handling the request to manage session cookies.
   *
   * @param request      The HTTP request.
   * @param requestLine  The request line of the HTTP request.
   * @param httpResponse The HTTP response.
   */
  private void processCookieAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse) {
    if (httpResponse == null || !httpConfig.isUseSession()) {
      return;
    }

    HttpSession httpSession = request.getHttpSession();
    String sessionId = HttpSessionUtils.getSessionId(request);
    if (StrUtil.isBlank(sessionId)) {
      createSessionCookie(request, httpSession, httpResponse, false);
    } else {
      HttpSession httpSessionFromStore = (HttpSession) httpConfig.getSessionStore().get(sessionId);
      if (httpSessionFromStore == null) { // Cookie exists but session has timed out
        createSessionCookie(request, httpSession, httpResponse, false);
      }
    }
  }

  /**
   * Creates a session cookie based on the session ID.
   *
   * @param request      The HTTP request.
   * @param httpSession  The HTTP session.
   * @param httpResponse The HTTP response.
   * @param forceCreate  Whether to force creation of the session cookie.
   */
  private void createSessionCookie(HttpRequest request, HttpSession httpSession, HttpResponse httpResponse,
      boolean forceCreate) {
    if (httpResponse == null) {
      return;
    }

    if (!forceCreate) {
      Object existingCookie = request.channelContext.getAttribute(DefaultHttpRequestConstants.SESSION_COOKIE_KEY);
      if (existingCookie != null) {
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
  }

  /**
   * Updates the session ID for the given HTTP session.
   *
   * @param request      The HTTP request.
   * @param httpSession  The HTTP session.
   * @param httpResponse The HTTP response.
   * @return The updated HttpSession.
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
    httpSession.update(httpConfig); // Update session changes
    return httpSession;
  }

  /**
   * Processes cookies before handling the request to manage session retrieval or
   * creation.
   *
   * @param request     The HTTP request.
   * @param requestLine The request line of the HTTP request.
   * @throws ExecutionException If an error occurs during processing.
   */
  private void processCookieBeforeHandler(HttpRequest request, RequestLine requestLine) throws ExecutionException {
    if (!httpConfig.isUseSession()) {
      return;
    }

    String sessionId = HttpSessionUtils.getSessionId(request);
    HttpSession httpSession = null;
    if (StrUtil.isBlank(sessionId)) {
      httpSession = createSession(request);
    } else {
      httpSession = (HttpSession) httpConfig.getSessionStore().get(sessionId);
      if (httpSession == null) {
        if (log.isDebugEnabled()) {
          log.info("Session ID '{}' has timed out for channel: {}", sessionId, request.channelContext);
        }
        httpSession = createSession(request);
      }
    }
    request.setHttpSession(httpSession);
  }

  /**
   * Generates a 404 Not Found response.
   *
   * @param request     The HTTP request.
   * @param requestLine The request line of the HTTP request.
   * @return The 404 HttpResponse.
   * @throws Exception If an error occurs during response generation.
   */
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
    HttpResponse httpResponse = TioRequestContext.getResponse();
    if (httpResponse != null) {
      return Resps.resp404(httpResponse, requestLine, httpConfig);
    } else {
      return Resps.resp404(request, requestLine, httpConfig);

    }
  }

  /**
   * Generates a 500 Internal Server Error response.
   *
   * @param request     The HTTP request.
   * @param requestLine The request line of the HTTP request.
   * @param throwable   The exception that occurred.
   * @return The 500 HttpResponse.
   * @throws Exception If an error occurs during response generation.
   */
  @Override
  public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append(SysConst.CRLF).append("Remote Address: ").append(request.getClientIp());
    sb.append(SysConst.CRLF).append("Request: ").append(requestLine.toString());
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
   * Sets the HTTP configuration.
   *
   * @param httpConfig The HttpConfig to set.
   */
  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  /**
   * Sets the HTTP server interceptor.
   *
   * @param httpServerInterceptor The HttpRequestInterceptor to set.
   */
  public void setHttpServerInterceptor(HttpRequestInterceptor httpServerInterceptor) {
    this.httpRequestInterceptor = httpServerInterceptor;
  }

  /**
   * Clears the static resource cache.
   */
  @Override
  public void clearStaticResCache() {
    AbsCache staticResCache = StaticResourcesCache.getStaticResCache();
    if (staticResCache != null) {
      staticResCache.clear();
    }
  }

  /**
   * Retrieves the HTTP session listener.
   *
   * @return The HttpSessionListener.
   */
  public HttpSessionListener getHttpSessionListener() {
    return httpSessionListener;
  }

  /**
   * Sets the HTTP session listener.
   *
   * @param httpSessionListener The HttpSessionListener to set.
   */
  public void setHttpSessionListener(HttpSessionListener httpSessionListener) {
    this.httpSessionListener = httpSessionListener;
  }

  /**
   * Retrieves the session cookie decorator.
   *
   * @return The SessionCookieDecorator.
   */
  public SessionCookieDecorator getSessionCookieDecorator() {
    return sessionCookieDecorator;
  }

  /**
   * Sets the session cookie decorator.
   *
   * @param sessionCookieDecorator The SessionCookieDecorator to set.
   */
  public void setSessionCookieDecorator(SessionCookieDecorator sessionCookieDecorator) {
    this.sessionCookieDecorator = sessionCookieDecorator;
  }

  /**
   * Retrieves the IP path access statistics.
   *
   * @return The IpPathAccessStats.
   */
  public IpPathAccessStats getIpPathAccessStats() {
    return ipPathAccessStats;
  }

  /**
   * Sets the IP path access statistics.
   *
   * @param ipPathAccessStats The IpPathAccessStats to set.
   */
  public void setIpPathAccessStats(IpPathAccessStats ipPathAccessStats) {
    this.ipPathAccessStats = ipPathAccessStats;
  }

  /**
   * Retrieves the throwable handler.
   *
   * @return The ThrowableHandler.
   */
  public ThrowableHandler getThrowableHandler() {
    return throwableHandler;
  }

  /**
   * Sets the throwable handler.
   *
   * @param throwableHandler The ThrowableHandler to set.
   */
  public void setThrowableHandler(ThrowableHandler throwableHandler) {
    this.throwableHandler = throwableHandler;
  }

  /**
   * Retrieves the token path access statistics.
   *
   * @return The TokenPathAccessStats.
   */
  public TokenPathAccessStats getTokenPathAccessStats() {
    return tokenPathAccessStats;
  }

  /**
   * Sets the token path access statistics.
   *
   * @param tokenPathAccessStats The TokenPathAccessStats to set.
   */
  public void setTokenPathAccessStats(TokenPathAccessStats tokenPathAccessStats) {
    this.tokenPathAccessStats = tokenPathAccessStats;
  }

  /**
   * Checks if compatibility assignment is enabled.
   *
   * @return True if enabled, false otherwise.
   */
  public boolean isCompatibilityAssignment() {
    return compatibilityAssignment;
  }

  /**
   * Sets the compatibility assignment flag.
   *
   * @param compatibilityAssignment The compatibilityAssignment to set.
   */
  public void setCompatibilityAssignment(boolean compatibilityAssignment) {
    this.compatibilityAssignment = compatibilityAssignment;
  }

  /**
   * Logs the interceptor report details.
   *
   * @param requestLine  The request line.
   * @param httpResponse The HTTP response.
   */
  private void logInterceptorReport(RequestLine requestLine, HttpResponse httpResponse) {
    if (log.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n-----------HTTP Request Interceptor Report---------------------\n");
      sb.append("Request: ").append(requestLine.toString()).append("\n").append("Interceptor: ")
          .append(httpRequestInterceptor).append("\n").append("Response: ").append(httpResponse).append("\n\n");
      log.info(sb.toString());
    }
  }

  /**
   * Logs the router report details.
   *
   * @param requestLine        The request line.
   * @param httpRequestHandler The HTTP request handler.
   * @param routerName         The name of the router.
   */
  private void logRouterReport(RequestLine requestLine, HttpRequestHandler httpRequestHandler, String routerName) {
    if (printReport && log.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n-----------").append(routerName).append(" Report---------------------\n");
      sb.append("Request: ").append(requestLine.toString()).append("\n").append("Handler: ")
          .append(httpRequestHandler.toString()).append("\n");
      log.info(sb.toString());
    }
  }

  /**
   * Logs the function router report details.
   *
   * @param requestLine   The request line.
   * @param functionEntry The function route entry.
   */
  private void logFunctionRouterReport(RequestLine requestLine, RouteEntry<Object, Object> functionEntry) {
    if (printReport && log.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n-----------HTTP Request Function Router Report---------------------\n");
      sb.append("Request: ").append(requestLine.toString()).append("\n").append("Function Entry: ")
          .append(functionEntry.toString()).append("\n");
      log.info(sb.toString());
    }
  }

  /**
   * Logs the action report details.
   *
   * @param requestLine The request line.
   * @param method      The action method.
   */
  private void logActionReport(RequestLine requestLine, Method method) {
    if (printReport && log.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n-----------Action Report---------------------\n");
      sb.append("Request: ").append(requestLine.toString()).append("\n").append("Method: ").append(method.toString())
          .append("\n");
      log.info(sb.toString());
    }
  }
}
