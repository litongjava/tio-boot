package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.annotation.AImport;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.process.BeforeStartConfigurationProcess;
import com.litongjava.jfinal.aop.process.ComponentAnnotation;
import com.litongjava.jfinal.aop.scaner.ComponentScanner;
import com.litongjava.tio.boot.constatns.AopClasses;
import com.litongjava.tio.boot.constatns.TioBootConfigKeys;
import com.litongjava.tio.boot.http.handler.AopControllerFactory;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.http.handler.RequestStatisticsHandler;
import com.litongjava.tio.boot.http.handler.ResponseStatisticsHandler;
import com.litongjava.tio.boot.http.handler.TioServerSessionRateLimiter;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpRequestInterceptorDispatcher;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoute;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerHandlerListener;
import com.litongjava.tio.boot.server.TioBootServerListener;
import com.litongjava.tio.boot.tcp.ServerTcpHandler;
import com.litongjava.tio.boot.utils.ClassCheckUtils;
import com.litongjava.tio.boot.websocket.handler.DefaultWebSocketHandlerDispather;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.http.server.annotation.RequestPath;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.mvc.intf.ControllerFactory;
import com.litongjava.tio.http.server.router.DefaultHttpReqeustRoute;
import com.litongjava.tio.http.server.router.HttpReqeustGroovyRoute;
import com.litongjava.tio.http.server.router.RequestRoute;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.Threads;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import com.litongjava.tio.utils.environment.EnvUtils;
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
    return run(primarySources, null, args);
  }

  @Override
  public Context run(Class<?>[] primarySources, TioBootConfiguration tioBootConfiguration, String[] args) {
    long scanClassStartTime = 0L;
    long scanClassEndTime = 0L;
    long configStartTime = 0L;
    long configEndTimeTime = 0L;

    long initServerStartTime = System.currentTimeMillis();
    EnvUtils.buildCmdArgsMap(args);
    EnvUtils.load();

    // port and contextPath
    int port = EnvUtils.getInt(TioBootConfigKeys.SERVER_PORT, 80);
    String contextPath = EnvUtils.get(TioBootConfigKeys.SERVER_CONTEXT_PATH);

    TioBootServer tioBootServer = TioBootServer.me();

    // httpConfig
    HttpConfig httpConfig = configHttp(port, contextPath);
    httpConfig.setBindIp(EnvUtils.get(TioBootConfigKeys.SERVER_ADDRESS));

    // http request routes
    TioBootHttpControllerRoute tioBootHttpControllerRoutes = new TioBootHttpControllerRoute();
    tioBootServer.setTioBootHttpRoutes(tioBootHttpControllerRoutes);

    // cacheFactory
    ConcurrentMapCacheFactory cacheFactory = ConcurrentMapCacheFactory.INSTANCE;

    // defaultHttpRequestHandlerDispather
    HttpRequestHandler usedHttpRequestHandler = tioBootServer.getHttpRequestHandler();
    if (usedHttpRequestHandler == null) {
      DefaultHttpRequestHandler defaultHttpRequestHandler = new DefaultHttpRequestHandler();
      tioBootServer.setDefaultHttpRequestHandler(defaultHttpRequestHandler);
      usedHttpRequestHandler = defaultHttpRequestHandler;
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
        usedHttpRequestHandler, serverTcpHandler);

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
    serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, usedHttpRequestHandler);

    // TioServer
    tioBootServer.init(serverTioConfig, wsServerConfig, httpConfig);

    // defaultHttpServerInterceptorDispather
    HttpRequestInterceptor defaultHttpServerInterceptorDispather = tioBootServer
        .getDefaultHttpRequestInterceptorDispatcher();

    if (defaultHttpServerInterceptorDispather == null) {
      defaultHttpServerInterceptorDispather = new DefaultHttpRequestInterceptorDispatcher();
      tioBootServer.setDefaultHttpRequestInterceptorDispatcher(defaultHttpServerInterceptorDispather);
    }

    // httpReqeustSimpleHandlerRoute
    RequestRoute httpReqeustSimpleHandlerRoute = tioBootServer.getRequestRoute();
    if (httpReqeustSimpleHandlerRoute == null) {
      httpReqeustSimpleHandlerRoute = new DefaultHttpReqeustRoute();
      tioBootServer.setRequestRoute(httpReqeustSimpleHandlerRoute);
    }

    long initServerEndTime = System.currentTimeMillis();

    List<Class<?>> scannedClasses = null;
    boolean printScannedClasses = EnvUtils.getBoolean(TioBootConfigKeys.AOP_PRINT_SCANNED_CLASSSES, false);
    // 添加自定义组件注解
    if (ClassCheckUtils.check(AopClasses.Aop)) {
      scanClassStartTime = System.currentTimeMillis();
      ComponentAnnotation.addComponentAnnotation(RequestPath.class);
      // process @AComponentScan
      try {
        scannedClasses = ComponentScanner.scan(primarySources, printScannedClasses);
      } catch (Exception e1) {
        e1.printStackTrace();
      }

      // process @Improt
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
      scanClassEndTime = System.currentTimeMillis();

    }

    configStartTime = System.currentTimeMillis();
    if (tioBootConfiguration != null) {
      try {
        tioBootConfiguration.config();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (scannedClasses != null && scannedClasses.size() > 0) {
      this.initAnnotation(scannedClasses);
    }

    HttpReqeustGroovyRoute httpReqeustGroovyRoute = tioBootServer.getHttpReqeustGroovyRoute();
    RequestStatisticsHandler requestStatisticsHandler = tioBootServer.getRequestStatisticsHandler();
    ResponseStatisticsHandler responseStatisticsHandler = tioBootServer.getResponseStatisticsHandler();

    if (usedHttpRequestHandler instanceof DefaultHttpRequestHandler) {
      ((DefaultHttpRequestHandler) usedHttpRequestHandler).init(httpConfig, tioBootHttpControllerRoutes,
          defaultHttpServerInterceptorDispather, httpReqeustSimpleHandlerRoute, httpReqeustGroovyRoute, cacheFactory,
          requestStatisticsHandler, responseStatisticsHandler);
    }

    configEndTimeTime = System.currentTimeMillis();

    long serverStartTime = System.currentTimeMillis();

    TioBootServerListener serverListener = tioBootServer.getTioBootServerListener();
    if (serverListener != null) {
      serverListener.boforeStart(primarySources, args);
    }

    // 根据参数判断是否启动服务器,默认启动服务器
    if (!EnvUtils.getBoolean(TioBootConfigKeys.TIO_NO_SERVER, false)) {

      // start server
      try {
        tioBootServer.start(httpConfig.getBindIp(), httpConfig.getBindPort());
        TioBootServerListener tioBootServerListener = tioBootServer.getTioBootServerListener();
        if (serverListener != null) {
          tioBootServerListener.afterStarted(primarySources, args, this);
        }
      } catch (IOException e) {
        e.printStackTrace();
        this.close();
        System.exit(1);
      }
    }

    long serverEndTime = System.currentTimeMillis();

    long routeStartTime = System.currentTimeMillis();
    // 根据参数判断是否初始化路由,默认初始化路由
    if (!EnvUtils.getBoolean(TioBootConfigKeys.TIO_NO_SERVER, false)) {
      if (tioBootHttpControllerRoutes != null) {
        ControllerFactory aopFactory = new AopControllerFactory();
        tioBootHttpControllerRoutes.addRoutes(scannedClasses, aopFactory);
      }
    }
    long routeEndTime = System.currentTimeMillis();

    log.info("init:{}(ms),scan class:{}(ms),config:{}(ms),server:{}(ms),http route:{}(ms)",
        initServerEndTime - initServerStartTime, scanClassEndTime - scanClassStartTime,
        configEndTimeTime - configStartTime, serverEndTime - serverStartTime, routeEndTime - routeStartTime);

    if (!EnvUtils.getBoolean(TioBootConfigKeys.TIO_NO_SERVER, false)) {
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
      if (ClassCheckUtils.check(AopClasses.Aop)) {
        Aop.close();
      }
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
    String pageRoot = EnvUtils.get(TioBootConfigKeys.SERVER_RESOURCES_STATIC_LOCATIONS, "classpath:/pages");
    // httpConfig
    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // maxLiveTimeOfStaticRes
    Integer maxLiveTimeOfStaticRes = EnvUtils.getInt(TioBootConfigKeys.HTTP_MAX_LIVE_TIME_OF_STATIC_RES);
    String page404 = EnvUtils.get(TioBootConfigKeys.SERVER_404);
    String page500 = EnvUtils.get(TioBootConfigKeys.SERVER_500);
    if (maxLiveTimeOfStaticRes != null) {
      httpConfig.setMaxLiveTimeOfStaticRes(maxLiveTimeOfStaticRes);
    }
    if (page404 != null) {
      httpConfig.setPage404(page404);
    }

    if (page500 != null) {
      httpConfig.setPage500(page500);
    }
    httpConfig.setUseSession(EnvUtils.getBoolean(TioBootConfigKeys.HTTP_ENABLE_SESSION, true));
    httpConfig.setCheckHost(EnvUtils.getBoolean(TioBootConfigKeys.HTTP_CHECK_HOST, false));
    // httpMultipartMaxRequestZize
    Integer httpMultipartMaxRequestZize = EnvUtils.getInt(TioBootConfigKeys.HTTP_MULTIPART_MAX_REQUEST_SIZE);
    if (httpMultipartMaxRequestZize != null) {
      httpConfig.setMaxLengthOfPostBody(httpMultipartMaxRequestZize);
    }
    // httpMultipartMaxFileZize
    Integer httpMultipartMaxFileZize = EnvUtils.getInt(TioBootConfigKeys.HTTP_MULTIPART_MAX_FILE_ZIZE);
    if (httpMultipartMaxFileZize != null) {
      httpConfig.setMaxLengthOfMultiBody(httpMultipartMaxFileZize);
    }

    if (EnvUtils.getBoolean(TioBootConfigKeys.HTTP_ENABLE_REQUEST_LIMIT, true)) {
      httpConfig.setSessionRateLimiter(new TioServerSessionRateLimiter());
    }
    return httpConfig;
  }
}