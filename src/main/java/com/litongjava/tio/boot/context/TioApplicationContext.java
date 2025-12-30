package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.litongjava.annotation.AImport;
import com.litongjava.annotation.RequestPath;
import com.litongjava.constants.AopClasses;
import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.context.BootConfiguration;
import com.litongjava.context.Context;
import com.litongjava.context.ServerListener;
import com.litongjava.controller.ControllerFactory;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.context.AopContext;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.process.BeforeStartConfigurationProcess;
import com.litongjava.jfinal.aop.process.ComponentAnnotation;
import com.litongjava.jfinal.aop.scanner.ComponentScanner;
import com.litongjava.jfinal.aop.scanner.DefaultComponentScanner;
import com.litongjava.tio.boot.decode.TioDecodeExceptionHandler;
import com.litongjava.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.http.handler.internal.AopControllerFactory;
import com.litongjava.tio.boot.http.handler.internal.RequestStatisticsHandler;
import com.litongjava.tio.boot.http.handler.internal.ResponseStatisticsHandler;
import com.litongjava.tio.boot.http.handler.internal.StaticResourceHandler;
import com.litongjava.tio.boot.http.handler.internal.TioBootHttpRequestDispatcher;
import com.litongjava.tio.boot.http.handler.internal.TioServerSessionRateLimiter;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpRequestInterceptorDispatcher;
import com.litongjava.tio.boot.server.TioBootAioListener;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.swagger.TioSwaggerGenerateUtils;
import com.litongjava.tio.boot.swagger.TioSwaggerV2Config;
import com.litongjava.tio.boot.utils.ClassCheckUtils;
import com.litongjava.tio.boot.websocket.DefaultWebSocketRouter;
import com.litongjava.tio.boot.websocket.TioBootWebSocketDispatcher;
import com.litongjava.tio.boot.websocket.WebSocketRouter;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.router.DefaultHttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.DefaultHttpRequestRouter;
import com.litongjava.tio.http.server.router.HttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpRequestGroovyRouter;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCacheFactory;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import com.litongjava.tio.utils.cache.redismap.RedisMapCacheFactory;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.MapJsonUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.litongjava.tio.websocket.common.WebSocketSnowflakeId;
import com.litongjava.tio.websocket.server.WebsocketServerConfig;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioApplicationContext implements Context {

  private final TioBootServer tioBootServer = TioBootServer.me();
  private int port;

  /**
   * 1. Pre-server startup configuration 2. Start the server 3. Initialize
   * configuration classes 4. Initialize component classes 5. Add routes
   */
  @Override
  public Context run(Class<?>[] primarySources, String[] args) {
    return run(primarySources, null, args);
  }

  @Override
  public Context run(Class<?>[] primarySources, BootConfiguration tioBootConfiguration, String[] args) {
    long scanClassStartTime = 0L;
    long scanClassEndTime = 0L;

    // Build command arguments and load environment variables
    EnvUtils.buildCmdArgsMap(args);
    EnvUtils.load();
    TioThreadUtils.start();

    List<Class<?>> scannedClasses = null;
    boolean printScannedClasses = EnvUtils.getBoolean(ServerConfigKeys.AOP_PRINT_SCANNED_CLASSES, false);

    // Add custom component annotations if AOP is enabled
    if (ClassCheckUtils.check(AopClasses.AOP)) {
      scanClassStartTime = System.currentTimeMillis();
      ComponentAnnotation.addComponentAnnotation(RequestPath.class);

      // Process @AComponentScan
      ComponentScanner componentScanner = AopContext.me().getComponentScanner();
      if (componentScanner != null) {
        try {
          scannedClasses = componentScanner.scan(primarySources, printScannedClasses);
        } catch (Exception e) {

        }
      } else {
        try {
          scannedClasses = new DefaultComponentScanner().scan(primarySources, printScannedClasses);
        } catch (Exception e) {
          log.error("Error during component scanning", e);
        }
      }

      if (scannedClasses != null) {
        log.info("Scanned classes count: {}", scannedClasses.size());
      }

      // Process @AImport
      for (Class<?> primarySource : primarySources) {
        if (primarySource.isAnnotationPresent(AImport.class)) {
          AImport importAnnotation = primarySource.getAnnotation(AImport.class);
          Class<?>[] imports = importAnnotation.value();
          for (Class<?> clazz : imports) {
            scannedClasses.add(clazz);
          }
        }
      }

      scannedClasses = processBeforeStartConfiguration(scannedClasses);
      scanClassEndTime = System.currentTimeMillis();
    } else {
      log.info("AOP class not found: {}", AopClasses.AOP);
    }

    long initServerStartTime = System.currentTimeMillis();

    // Configure port and context path
    port = EnvUtils.getInt(ServerConfigKeys.SERVER_PORT, 80);
    String contextPath = EnvUtils.get(ServerConfigKeys.SERVER_CONTEXT_PATH, "");

    // Configure HTTP settings
    HttpConfig httpConfig = configureHttp(port, contextPath);
    httpConfig.setBindIp(EnvUtils.get(ServerConfigKeys.SERVER_ADDRESS, "0.0.0.0"));

    // Initialize HTTP controller router
    TioBootHttpControllerRouter controllerRouter = new TioBootHttpControllerRouter();
    tioBootServer.setControllerRouter(controllerRouter);

    // Configure cache factory
    CacheFactory cacheFactory = determineCacheFactory();

    log.info("Using cache: {}", cacheFactory.getClass());

    // Initialize default HTTP request dispatcher if not set
    ITioHttpRequestHandler usedHttpRequestHandler = tioBootServer.getHttpRequestDispatcher();
    if (usedHttpRequestHandler == null) {
      usedHttpRequestHandler = new TioBootHttpRequestDispatcher();
      tioBootServer.setHttpRequestDispatcher(usedHttpRequestHandler);
    }

    // Configure WebSocket
    IWebSocketHandler defaultWebSocketHandler = tioBootServer.getWebSocketHandlerDispatcher();
    WebSocketRouter webSocketRouter = tioBootServer.getWebSocketRouter();

    if (defaultWebSocketHandler == null) {
      TioBootWebSocketDispatcher webSocketDispatcher = new TioBootWebSocketDispatcher();
      if (webSocketRouter == null) {
        webSocketRouter = new DefaultWebSocketRouter();
      }
      webSocketDispatcher.setWebSocketRouter(webSocketRouter);
      tioBootServer.setWebSocketRouter(webSocketRouter);
      defaultWebSocketHandler = webSocketDispatcher;
      tioBootServer.setWebSocketHandlerDispatcher(defaultWebSocketHandler);
    }

    WebsocketServerConfig wsServerConfig = new WebsocketServerConfig(port);

    // Configure TCP
    ServerAioHandler serverAioHandler = tioBootServer.getServerAioHandler();
    TioDecodeExceptionHandler decodeExceptionHandler = tioBootServer.getDecodeExceptionHandler();

    // Initialize server handler
    TioBootServerHandler serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebSocketHandler, httpConfig,
        usedHttpRequestHandler, serverAioHandler, decodeExceptionHandler);

    // Initialize server listener
    ServerAioListener externalServerListener = tioBootServer.getServerAioListener();
    ServerAioListener serverAioListener = new TioBootAioListener(externalServerListener);

    // Configure server settings
    ServerTioConfig serverTioConfig = new ServerTioConfig("tio-boot");
    serverTioConfig.setServerAioListener(serverAioListener);
    serverTioConfig.setServerAioHandler(serverHandler);
    serverTioConfig.setCacheFactory(cacheFactory);
    serverTioConfig.setDefaultIpRemovalListenerWrapper();
    serverTioConfig.statOn = EnvUtils.getBoolean(ServerConfigKeys.SERVER_STAT_ENABLE, false);
    Integer workerThreads = EnvUtils.getInteger(ServerConfigKeys.SERVER_WORKER_THREADS);

    if (workerThreads != null) {
      serverTioConfig.setWorkerThreads(workerThreads);
    }

    // Configure heartbeat
    int heartbeatTimeout = EnvUtils.getInt(ServerConfigKeys.SERVER_HEARTBEAT_TIMEOUT, 0);
    log.info("Server heartbeat timeout: {}", heartbeatTimeout);
    serverTioConfig.setHeartbeatTimeout(heartbeatTimeout);

    // Set UUID generator for WebSocket
    WebSocketSnowflakeId snowflakeId = new WebSocketSnowflakeId();
    serverTioConfig.setTioUuid(snowflakeId);

    // Configure buffer size
    serverTioConfig.setReadBufferSize(EnvUtils.getInt(ServerConfigKeys.SERVER_READ_BUFFER_SIZE, 1024 * 30));

    // Set HTTP request handler attribute
    serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, usedHttpRequestHandler);

    // Configure session if enabled
    if (httpConfig.isUseSession()) {
      if (httpConfig.getSessionStore() == null) {
        long sessionTimeout = httpConfig.getSessionTimeout();
        AbsCache sessionCache = cacheFactory.register(httpConfig.getSessionCacheName(), null, sessionTimeout);
        httpConfig.setSessionStore(sessionCache);
      }

      if (httpConfig.getSessionIdGenerator() == null) {
        httpConfig.setSessionIdGenerator(UUIDSessionIdGenerator.INSTANCE);
      }
    }

    // Initialize TioBoot server
    tioBootServer.init(serverTioConfig, wsServerConfig, httpConfig);

    // Initialize default HTTP request interceptor dispatcher if not set
    HttpRequestInterceptor defaultHttpInterceptorDispatcher = tioBootServer.getHttpRequestInterceptorDispatcher();
    if (defaultHttpInterceptorDispatcher == null) {
      defaultHttpInterceptorDispatcher = new DefaultHttpRequestInterceptorDispatcher();
      tioBootServer.setHttpRequestInterceptorDispatcher(defaultHttpInterceptorDispatcher);
    }

    // Initialize HTTP request routers if not set
    HttpRequestRouter httpRequestRouter = tioBootServer.getRequestRouter();
    if (httpRequestRouter == null) {
      httpRequestRouter = new DefaultHttpRequestRouter();
      tioBootServer.setRequestRouter(httpRequestRouter);
    }

    HttpRequestFunctionRouter requestFunctionRouter = tioBootServer.getRequestFunctionRouter();
    if (requestFunctionRouter == null) {
      requestFunctionRouter = new DefaultHttpRequestFunctionRouter();
      tioBootServer.setRequestFunctionRouter(requestFunctionRouter);
    }

    long initServerEndTime = System.currentTimeMillis();
    long configStartTime = System.currentTimeMillis();

    // 1. BootConfiguration.config()
    long bootConfigStart = 0L;
    long bootConfigEnd = 0L;
    if (tioBootConfiguration != null) {
      try {
        bootConfigStart = System.currentTimeMillis();
        tioBootConfiguration.config();
        bootConfigEnd = System.currentTimeMillis();
      } catch (Exception e) {
        if (EnvUtils.getBoolean(ServerConfigKeys.BOOT_EXCEPTION_EXIT, true)) {
          log.error("Failed to configure BootConfiguration:", e);
          System.exit(1);
        } else {
          throw new RuntimeException("Failed to configure BootConfiguration", e);
        }
      }
    }

    // 2. AOP 注解初始化
    long annotationStart = 0L;
    long annotationEnd = 0L;
    if (ClassCheckUtils.check(AopClasses.AOP) && scannedClasses != null && !scannedClasses.isEmpty()) {
      annotationStart = System.currentTimeMillis();
      initAnnotation(scannedClasses);
      annotationEnd = System.currentTimeMillis();
    }

    // 3. 拿路由、拦截器等（通常很快）
    HttpRequestGroovyRouter groovyRouter = tioBootServer.getRequestGroovyRouter();
    RequestStatisticsHandler requestStatisticsHandler = tioBootServer.getRequestStatisticsHandler();
    ResponseStatisticsHandler responseStatisticsHandler = tioBootServer.getResponseStatisticsHandler();
    HttpRequestHandler forwardHandler = tioBootServer.getForwardHandler();
    HttpRequestHandler notFoundHandler = tioBootServer.getNotFoundHandler();
    StaticResourceHandler staticResourceHandler = tioBootServer.getStaticResourceHandler();
    HttpRequestInterceptor httpRequestValidationInterceptor = tioBootServer.getHttpRequestValidationInterceptor();
    HttpRequestInterceptor authTokenInterceptor = tioBootServer.getAuthTokenInterceptor();

    // 4. Http dispatcher 初始化
    long dispatcherInitStart = 0L;
    long dispatcherInitEnd = 0L;
    if (usedHttpRequestHandler instanceof TioBootHttpRequestDispatcher) {
      dispatcherInitStart = System.currentTimeMillis();
      ((TioBootHttpRequestDispatcher) usedHttpRequestHandler).init(httpConfig, cacheFactory, defaultHttpInterceptorDispatcher,
          httpRequestValidationInterceptor, authTokenInterceptor, httpRequestRouter, groovyRouter, requestFunctionRouter, controllerRouter,
          forwardHandler, notFoundHandler, requestStatisticsHandler, responseStatisticsHandler, staticResourceHandler);
      dispatcherInitEnd = System.currentTimeMillis();
    }

    long configEndTime = System.currentTimeMillis();

    // 计算分项时间
    long bootConfigTime = bootConfigEnd - bootConfigStart;
    long annotationTime = annotationEnd - annotationStart;
    long dispatcherInitTime = dispatcherInitEnd - dispatcherInitStart;

    log.info("Config time detail (ms): total:{}, bootConfig:{}, initAnnotation:{}, dispatcherInit:{}",
        //
        (configEndTime - configStartTime),
        //
        bootConfigTime, annotationTime, dispatcherInitTime);

    long serverStartTime = System.currentTimeMillis();

    // Invoke server listener before starting
    ServerListener serverListener = tioBootServer.getTioBootServerListener();
    if (serverListener != null) {
      serverListener.beforeStart(primarySources, args);
    }

    // Determine whether to start the server based on configuration
    boolean shouldStartServer = EnvUtils.getBoolean(ServerConfigKeys.SERVER_LISTENING_ENABLE, true);
    if (shouldStartServer) {
      try {
        tioBootServer.start(httpConfig.getBindIp(), httpConfig.getBindPort());
        // Invoke server listener after starting
        if (serverListener != null) {
          serverListener.afterStarted(primarySources, args, this);
        }
      } catch (IOException e) {
        log.error("Failed to start server port:{}", port, e);
        close();
        System.exit(1);
      }
    }

    long serverEndTime = System.currentTimeMillis();

    long routeStartTime = System.currentTimeMillis();

    // Initialize controllers if server is listening
    if (shouldStartServer) {
      // Log WebSocket mappings
      Map<String, IWebSocketHandler> webSocketMapping = webSocketRouter.all();
      if (!webSocketMapping.isEmpty()) {
        log.info("WebSocket handler:\n{}", MapJsonUtils.toPrettyJson(webSocketMapping));
      }

      // Log HTTP mappings
      Map<String, HttpRequestHandler> httpMapping = httpRequestRouter.all();
      if (!httpMapping.isEmpty()) {
        log.info("HTTP handler:\n{}", MapJsonUtils.toPrettyJson(httpMapping));
      }

      if (controllerRouter != null && scannedClasses != null) {
        ControllerFactory aopFactory = new AopControllerFactory();
        if (!scannedClasses.isEmpty()) {
          controllerRouter.addControllers(scannedClasses);
        }
        controllerRouter.scan(aopFactory);

        // Generate Swagger documentation if enabled
        TioSwaggerV2Config swaggerV2Config = tioBootServer.getSwaggerV2Config();
        if (swaggerV2Config != null && swaggerV2Config.isEnable()) {
          String swaggerJson = TioSwaggerGenerateUtils.generateSwaggerJson(controllerRouter, swaggerV2Config.getApiInfo());
          swaggerV2Config.setSwaggerJson(swaggerJson);
        }
      }
    } else {
      log.info("{}:false", ServerConfigKeys.SERVER_LISTENING_ENABLE);
    }

    long routeEndTime = System.currentTimeMillis();

    // Calculate and log total initialization times
    long scanClassTime = scanClassEndTime - scanClassStartTime;
    long initServerTime = initServerEndTime - initServerStartTime;
    long configTime = configEndTime - configStartTime;
    long serverTime = serverEndTime - serverStartTime;
    long routeTime = routeEndTime - routeStartTime;

    log.info("Initialization times (ms): Total: {}, Scan Classes: {}, Init Server: {}, Config: {}, Server: {}, Route: {}",
        scanClassTime + initServerTime + configTime + serverTime + routeTime, scanClassTime, initServerTime, configTime, serverTime,
        routeTime);

    // Print URL if server is listening
    if (shouldStartServer) {
      printUrl(port, contextPath);
    }
    return this;
  }

  /**
   * Prints the server URL.
   *
   * @param port        The port number.
   * @param contextPath The context path.
   */
  private void printUrl(int port, String contextPath) {
    log.info("Server port: {}", port);
    StringBuilder fullUrl = new StringBuilder("http://localhost");
    if (port != 80) {
      fullUrl.append(":").append(port);
    }
    if (contextPath != null && !contextPath.isEmpty()) {
      if (!contextPath.startsWith("/")) {
        fullUrl.append("/");
      }
      fullUrl.append(contextPath);
    }
    log.info("Access URL: {}", fullUrl.toString());
  }

  /**
   * Processes configurations before starting the server.
   *
   * @param scannedClasses The list of scanned classes.
   * @return The processed list of classes.
   */
  private List<Class<?>> processBeforeStartConfiguration(List<Class<?>> scannedClasses) {
    BeforeStartConfigurationProcess beforeStartConfigurationProcess = new BeforeStartConfigurationProcess();
    try {
      return beforeStartConfigurationProcess.process(scannedClasses);
    } catch (Exception e) {
      if (EnvUtils.getBoolean(ServerConfigKeys.BOOT_EXCEPTION_EXIT, false)) {
        log.error("Failed to processBeforeStartConfiguration:", e);
        System.exit(1);
      } else {
        throw new RuntimeException(e);
      }
    }
    return null;

  }

  @Override
  public void initAnnotation(List<Class<?>> scannedClasses) {
    BeanProcess beanProcess = new BeanProcess();
    try {
      beanProcess.initAnnotation(scannedClasses);
    } catch (Exception e) {
      if (EnvUtils.getBoolean(ServerConfigKeys.BOOT_EXCEPTION_EXIT, true)) {
        log.error("Faild to initAnnotation,Exit JVM", e);
        System.exit(1);
      } else {
        throw new RuntimeException("Faild to initAnnotation", e);
      }

    }

  }

  @Override
  public void close() {
    log.info("Stopping server...");
    ServerListener serverListener = TioBootServer.me().getTioBootServerListener();
    try {
      if (serverListener != null) {
        serverListener.beforeStop();
      }
      tioBootServer.stop();
      TioThreadUtils.stop();
      if (ClassCheckUtils.check(AopClasses.AOP)) {
        Aop.close();
      }
      if (serverListener != null) {
        serverListener.afterStopped();
      }
    } catch (Exception e) {
      log.error("Error during server shutdown", e);
    }
  }

  @Override
  public boolean isRunning() {
    return tioBootServer.isRunning();
  }

  @Override
  public void restart(Class<?>[] primarySources, String[] args) {
    close();
    run(primarySources, args);
  }

  /**
   * Configures HTTP settings.
   *
   * @param port        The port number.
   * @param contextPath The context path.
   * @return The configured HttpConfig object.
   */
  private HttpConfig configureHttp(int port, String contextPath) {
    // Root directory for static resources like HTML/CSS/JS, supports classpath and
    // absolute paths
    String pageRoot = EnvUtils.get(ServerConfigKeys.SERVER_RESOURCES_STATIC_LOCATIONS, "classpath:/pages");

    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (Exception e) {
      log.error("Error setting page root", e);
    }
    boolean autoReoload = EnvUtils.getBoolean("server.resources.auto.reload");
    if (autoReoload || EnvUtils.isDevMode()) {
      httpConfig.setMonitorFileChange(autoReoload);
    }
    Integer maxLiveTimeOfStaticRes = EnvUtils.getInteger(ServerConfigKeys.HTTP_MAX_LIVE_TIME_OF_STATIC_RES);
    String page404 = EnvUtils.get(ServerConfigKeys.SERVER_404);
    String page500 = EnvUtils.get(ServerConfigKeys.SERVER_500);

    if (maxLiveTimeOfStaticRes != null) {
      httpConfig.setMaxLiveTimeOfStaticRes(maxLiveTimeOfStaticRes);
    }
    if (page404 != null) {
      httpConfig.setPage404(page404);
    }
    if (page500 != null) {
      httpConfig.setPage500(page500);
    }

    boolean enableSession = EnvUtils.getBoolean(ServerConfigKeys.SERVER_SESSION_ENABLE, false);
    log.info("Server session enabled: {}", enableSession);
    httpConfig.setUseSession(enableSession);
    httpConfig.setCheckHost(EnvUtils.getBoolean(ServerConfigKeys.HTTP_CHECK_HOST, false));

    // Configure multipart request sizes
    Integer multipartMaxRequestSize = EnvUtils.getInteger(ServerConfigKeys.HTTP_MULTIPART_MAX_REQUEST_SIZE);
    if (multipartMaxRequestSize != null && multipartMaxRequestSize > 0) {
      httpConfig.setMaxLengthOfPostBody(multipartMaxRequestSize);
    }

    Integer multipartMaxFileSize = EnvUtils.getInteger(ServerConfigKeys.HTTP_MULTIPART_MAX_FILE_SIZE);
    if (multipartMaxFileSize != null && multipartMaxFileSize > 0) {
      httpConfig.setMaxLengthOfMultiBody(multipartMaxFileSize);
    }

    // Enable request rate limiting if configured
    if (EnvUtils.getBoolean(ServerConfigKeys.HTTP_ENABLE_REQUEST_LIMIT, true)) {
      httpConfig.setSessionRateLimiter(new TioServerSessionRateLimiter());
    }

    return httpConfig;
  }

  /**
   * Determines the appropriate CacheFactory based on configuration and available
   * libraries.
   *
   * @return The selected CacheFactory instance.
   */
  private CacheFactory determineCacheFactory() {
    String cacheStore = EnvUtils.get("server.cache.store", "concurrent_map");
    CacheFactory cacheFactory;

    switch (cacheStore.toLowerCase()) {
    case "redis":
      if (ClassCheckUtils.check("com.litongjava.tio.utils.cache.redismap.RedisMapCacheFactory")) {
        cacheFactory = RedisMapCacheFactory.INSTANCE;
        break;
      }
      // Fallback if Redis is not available
    case "caffeine":
      if (ClassCheckUtils.check("com.github.benmanes.caffeine.cache.LoadingCache")) {
        cacheFactory = CaffeineCacheFactory.INSTANCE;
        break;
      }
      // Fallback if Caffeine is not available
    default:
      cacheFactory = ConcurrentMapCacheFactory.INSTANCE;
    }

    return cacheFactory;
  }

  @Override
  public int getPort() {
    return port;
  }
}