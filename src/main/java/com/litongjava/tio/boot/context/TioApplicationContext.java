package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.annotation.AImport;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.process.BeforeStartConfigurationProcess;
import com.litongjava.jfinal.aop.process.ComponentAnnotation;
import com.litongjava.jfinal.aop.scaner.ComponentScanner;
import com.litongjava.tio.boot.constatns.TioBootConfigKeys;
import com.litongjava.tio.boot.http.handler.AopControllerFactory;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandlerDispather;
import com.litongjava.tio.boot.http.handler.TioServerSessionRateLimiter;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpServerInterceptorDispatcher;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoutes;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerHandlerListener;
import com.litongjava.tio.boot.server.TioBootServerListener;
import com.litongjava.tio.boot.tcp.ServerTcpHandler;
import com.litongjava.tio.boot.websocket.handler.DefaultWebSocketHandlerDispather;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.http.server.annotation.RequestPath;
import com.litongjava.tio.http.server.mvc.intf.ControllerFactory;
import com.litongjava.tio.http.server.router.GroovyHttpRoutes;
import com.litongjava.tio.http.server.router.HttpRoutes;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.Threads;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import com.litongjava.tio.utils.environment.EnvironmentUtils;
import com.litongjava.tio.utils.thread.pool.SynThreadPoolExecutor;
import com.litongjava.tio.websocket.common.WsTioUuid;
import com.litongjava.tio.websocket.server.WsServerConfig;
import com.litongjava.tio.websocket.server.handler.IWsMsgHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioApplicationContext implements Context {

  /**
   * 1.服务启动前配置
   * 2.启动服务器
   * 3.初始配置类
   * 4.初始化组件类
   * 5.添加路由
   */
  @Override
  public Context run(Class<?>[] primarySources, String[] args) {
    long scanClassStartTime = System.currentTimeMillis();
    EnvironmentUtils.buildCmdArgsMap(args);

    EnvironmentUtils.load();

    List<Class<?>> scannedClasses = null;
    // 添加自定义组件注解
    ComponentAnnotation.addComponentAnnotation(RequestPath.class);
    boolean printScannedClasses = EnvironmentUtils.getBoolean(TioBootConfigKeys.AOP_PRINT_SCANNED_CLASSSES, false);
    // 执行组件扫描
    try {
      scannedClasses = ComponentScanner.scan(primarySources, printScannedClasses);
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    // 添加@Improt的类
    for (Class<?> primarySource : primarySources) {
      AImport importAnnotaion = primarySource.getAnnotation(AImport.class);
      if (importAnnotaion != null) {
        Class<?>[] value = importAnnotaion.value();
        for (Class<?> clazzz : value) {
          scannedClasses.add(clazzz);
        }
      }
    }
    scannedClasses = this.processBeforeStartConfiguration(scannedClasses);
    long scanClassEndTime = System.currentTimeMillis();

    long configStartTime = System.currentTimeMillis();
    this.initAnnotation(scannedClasses);
    long configEndTimeTime = System.currentTimeMillis();

    long serverStartTime = System.currentTimeMillis();
    TioBootServer tioBootServer = TioBootServer.me();

    TioBootServerListener serverListener = tioBootServer.getTioBootServerListener();
    if (serverListener != null) {
      serverListener.boforeStart(primarySources, args);
    }
    // 启动端口
    int port = EnvironmentUtils.getInt(TioBootConfigKeys.SERVER_PORT, 80);
    String contextPath = EnvironmentUtils.get(TioBootConfigKeys.SERVER_CONTEXT_PATH);
    // http request routes
    TioBootHttpControllerRoutes tioBootHttpControllerRoutes = new TioBootHttpControllerRoutes();
    // 添加到aop容器
    tioBootServer.setTioBootHttpRoutes(tioBootHttpControllerRoutes);

    // 根据参数判断是否启动服务器,默认启动服务器
    if (!EnvironmentUtils.getBoolean(TioBootConfigKeys.TIO_NO_SERVER, false)) {
      configAndStartServer(primarySources, args, port, contextPath, tioBootHttpControllerRoutes);
    }

    long serverEndTime = System.currentTimeMillis();

    long routeStartTime = System.currentTimeMillis();
    // 根据参数判断是否初始化路由,默认初始化路由
    if (!EnvironmentUtils.getBoolean(TioBootConfigKeys.TIO_NO_SERVER, false)) {
      if (tioBootHttpControllerRoutes != null) {
        ControllerFactory aopFactory = new AopControllerFactory();
        tioBootHttpControllerRoutes.addRoutes(scannedClasses, aopFactory);
      }
    }
    long routeEndTime = System.currentTimeMillis();

    log.info("scan class and init:{}(ms),config:{}(ms),server:{}(ms),http route:{}(ms)",
        scanClassEndTime - scanClassStartTime, configEndTimeTime - configStartTime, serverEndTime - serverStartTime,
        routeEndTime - routeStartTime);

    if (!EnvironmentUtils.getBoolean(TioBootConfigKeys.TIO_NO_SERVER, false)) {
      printUrl(port, contextPath);
    }

    return this;
  }

  /**
   * 打印启动端口和访问地址
   *
   * @param port
   * @param contextPath
   */
  private void printUrl(int port, String contextPath) {
    log.info("port:{}", port);
    String fullUrl = "http://localhost";
    if (port != 80) {
      fullUrl += (":" + port);
    }
    if (contextPath != null) {
      fullUrl += contextPath;
    }
    System.out.println(fullUrl);
  }

  /**
   * 配置并启动服务器
   *
   * @param primarySources
   * @param args
   * @param port
   * @param contextPath
   * @param tioBootHttpControllerRoutes
   */
  private void configAndStartServer(Class<?>[] primarySources, String[] args, int port, String contextPath,
      TioBootHttpControllerRoutes tioBootHttpControllerRoutes) {
    TioBootServer tioBootServer = TioBootServer.me();
    HttpConfig httpConfig = configHttp(port, contextPath);
    httpConfig.setBindIp(EnvironmentUtils.get(TioBootConfigKeys.SERVER_ADDRESS));

    // 第二个参数也可以是数组,自动配置扫描handler的路径
    ConcurrentMapCacheFactory cacheFactory = ConcurrentMapCacheFactory.INSTANCE;

    // defaultHttpServerInterceptorDispather
    DefaultHttpServerInterceptorDispatcher defaultHttpServerInterceptorDispather = tioBootServer
        .getDefaultHttpServerInterceptorDispatcher();

    if (defaultHttpServerInterceptorDispather == null) {
      defaultHttpServerInterceptorDispather = new DefaultHttpServerInterceptorDispatcher();
      tioBootServer.setDefaultHttpServerInterceptorDispatcher(defaultHttpServerInterceptorDispather);
    }

    // defaultHttpRequestHandlerDispather
    HttpRequestHandler defaultHttpRequestHandlerDispather = tioBootServer.getDefaultHttpRequestHandlerDispather();

    if (defaultHttpRequestHandlerDispather == null) {
      HttpRoutes httpRoutes = tioBootServer.getHttpRoutes();
      GroovyHttpRoutes dbRoutes = tioBootServer.getDbHttpRoutes();
      try {
        defaultHttpRequestHandlerDispather = new DefaultHttpRequestHandlerDispather(httpConfig, tioBootHttpControllerRoutes,
            defaultHttpServerInterceptorDispather, httpRoutes, dbRoutes, cacheFactory);
        
        tioBootServer.setDefaultHttpRequestHandlerDispather(defaultHttpRequestHandlerDispather);
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }

    // Executor
    SynThreadPoolExecutor tioExecutor = Threads.newTioExecutor();
    ThreadPoolExecutor gruopExecutor = Threads.newGruopExecutor();

    // config websocket
    IWsMsgHandler defaultWebScoketHanlder = tioBootServer.getDefaultWebSocketHandlerDispather();
    if (defaultWebScoketHanlder == null) {
      defaultWebScoketHanlder = new DefaultWebSocketHandlerDispather();
      tioBootServer.setDefaultWebSocketHandlerDispather(defaultWebScoketHanlder);
    }
    WsServerConfig wsServerConfig = new WsServerConfig(port);

    // config tcp
    ServerTcpHandler serverTcpHandler = tioBootServer.getServerTcpHandler();

    // serverHandler
    TioBootServerHandler serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig,
        defaultHttpRequestHandlerDispather, serverTcpHandler);

    // 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
    ServerAioListener externalServerListener = tioBootServer.getServerAioListener();
    ServerAioListener serverAioListener = new TioBootServerHandlerListener(externalServerListener);

    // 配置对象
    ServerTioConfig serverTioConfig = new ServerTioConfig("tio-boot", serverHandler, serverAioListener, tioExecutor,
        gruopExecutor, cacheFactory, null);

    if (httpConfig.isUseSession()) {
      if (httpConfig.getSessionStore() == null) {
        long sessionTimeout = httpConfig.getSessionTimeout();
        AbsCache caffeineCache = cacheFactory.register(httpConfig.getSessionCacheName(), null, sessionTimeout);
        httpConfig.setSessionStore(caffeineCache);
      }

      if (httpConfig.getSessionIdGenerator() == null) {
        httpConfig.setSessionIdGenerator(UUIDSessionIdGenerator.instance);
      }
    }

    // 设置心跳,-1 取消心跳
    serverTioConfig.setHeartbeatTimeout(0);
    WsTioUuid wsTioUuid = new WsTioUuid();
    serverTioConfig.setTioUuid(wsTioUuid);
    serverTioConfig.setReadBufferSize(1024 * 30);
    serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, defaultHttpRequestHandlerDispather);

    // TioServer对象
    tioBootServer.init(serverTioConfig, wsServerConfig, httpConfig);

    // 启动服务器
    try {
      tioBootServer.start(httpConfig.getBindIp(), httpConfig.getBindPort());
      TioBootServerListener serverListener = tioBootServer.getTioBootServerListener();
      if (serverListener != null) {
        serverListener.afterStarted(primarySources, args, this);
      }
    } catch (IOException e) {
      e.printStackTrace();
      this.close();
      System.exit(1);
    }
  }

  private List<Class<?>> processBeforeStartConfiguration(List<Class<?>> scannedClasses) {
    return new BeforeStartConfigurationProcess().process(scannedClasses);
  }

  @Override
  public void initAnnotation(List<Class<?>> scannedClasses) {
    new BeanProcess().initAnnotation(scannedClasses);
  }

  @Override
  public void close() {
    log.info("stop server");
    TioBootServerListener serverListener = TioBootServer.me().getTioBootServerListener();
    try {
      if (serverListener != null) {
        serverListener.beforeStop();
      }
      TioBootServer.me().stop();
      Aop.close();
      if (serverListener != null) {
        serverListener.afterStoped();
      }
    } catch (Exception e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @Override
  public boolean isRunning() {
    return TioBootServer.me().isRunning();
  }

  @Override
  public void restart(Class<?>[] primarySources, String[] args) {
    close();
    run(primarySources, args);
  }

  @Override
  public TioServer getServer() {
    return TioBootServer.me().getTioServer();
  }

  private HttpConfig configHttp(int port, String contextPath) {
    // html/css/js等的根目录，支持classpath:，也支持绝对路径
    String pageRoot = EnvironmentUtils.get(TioBootConfigKeys.SERVER_RESOURCES_STATIC_LOCATIONS, "classpath:/pages");
    // httpConfig
    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // maxLiveTimeOfStaticRes
    Integer maxLiveTimeOfStaticRes = EnvironmentUtils.getInt(TioBootConfigKeys.HTTP_MAX_LIVE_TIME_OF_STATIC_RES);
    String page404 = EnvironmentUtils.get(TioBootConfigKeys.SERVER_404);
    String page500 = EnvironmentUtils.get(TioBootConfigKeys.SERVER_500);
    if (maxLiveTimeOfStaticRes != null) {
      httpConfig.setMaxLiveTimeOfStaticRes(maxLiveTimeOfStaticRes);
    }
    Optional.ofNullable(page404).ifPresent((t) -> {
      httpConfig.setPage404(t);
    });
    Optional.ofNullable(page500).ifPresent((t) -> {
      httpConfig.setPage500(page500);
    });

    httpConfig.setUseSession(EnvironmentUtils.getBoolean(TioBootConfigKeys.HTTP_ENABLE_SESSION, true));
    httpConfig.setCheckHost(EnvironmentUtils.getBoolean(TioBootConfigKeys.HTTP_CHECK_HOST, false));
    // httpMultipartMaxRequestZize
    Integer httpMultipartMaxRequestZize = EnvironmentUtils.getInt(TioBootConfigKeys.HTTP_MULTIPART_MAX_REQUEST_SIZE);
    Optional.ofNullable(httpMultipartMaxRequestZize).ifPresent((t) -> {
      httpConfig.setMaxLengthOfPostBody(httpMultipartMaxRequestZize);
      log.info("set httpMultipartMaxRequestZize:{}", httpMultipartMaxRequestZize);
    });

    // httpMultipartMaxFileZize
    Integer httpMultipartMaxFileZize = EnvironmentUtils.getInt(TioBootConfigKeys.HTTP_MULTIPART_MAX_FILE_ZIZE);
    Optional.ofNullable(httpMultipartMaxFileZize).ifPresent((t) -> {
      httpConfig.setMaxLengthOfMultiBody(httpMultipartMaxFileZize);
      log.info("set httpMultipartMaxFileZize:{}", httpMultipartMaxFileZize);
    });

    if (EnvironmentUtils.getBoolean(TioBootConfigKeys.HTTP_ENABLE_REQUEST_LIMIT, true)) {
      httpConfig.setSessionRateLimiter(new TioServerSessionRateLimiter());
    }
    return httpConfig;
  }
}