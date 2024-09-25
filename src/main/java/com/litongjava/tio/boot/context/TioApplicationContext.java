package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;

import com.litongjava.annotation.RequestPath;
import com.litongjava.constatns.AopClasses;
import com.litongjava.constatns.ServerConfigKeys;
import com.litongjava.context.BootConfiguration;
import com.litongjava.context.Context;
import com.litongjava.context.ServerListener;
import com.litongjava.controller.ControllerFactory;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.annotation.AImport;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.process.BeforeStartConfigurationProcess;
import com.litongjava.jfinal.aop.process.ComponentAnnotation;
import com.litongjava.jfinal.aop.scaner.ComponentScanner;
import com.litongjava.tio.boot.http.handler.AopControllerFactory;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.http.handler.RequestStatisticsHandler;
import com.litongjava.tio.boot.http.handler.ResponseStatisticsHandler;
import com.litongjava.tio.boot.http.handler.TioServerSessionRateLimiter;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpRequestInterceptorDispatcher;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerHandlerListener;
import com.litongjava.tio.boot.utils.ClassCheckUtils;
import com.litongjava.tio.boot.websocket.handler.DefaultWebSocketHandlerDispather;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.router.DefaultHttpReqeustRouter;
import com.litongjava.tio.http.server.router.DefaultHttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpReqeustGroovyRouter;
import com.litongjava.tio.http.server.router.HttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.litongjava.tio.websocket.common.SnowflakeId;
import com.litongjava.tio.websocket.server.WebsocketServerConfig;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioApplicationContext implements Context {

  private TioBootServer tioBootServer = TioBootServer.me();
  private int port;

  /**
   * 1.服务启动前配置 2.启动服务器 3.初始配置类 4.初始化组件类 5.添加路由
   */
  @Override
  public Context run(Class<?>[] primarySources, String[] args) {
    return run(primarySources, null, args);
  }

  @Override
  public Context run(Class<?>[] primarySources, BootConfiguration tioBootConfiguration, String[] args) {
    long scanClassStartTime = 0L;
    long scanClassEndTime = 0L;
    long configStartTime = 0L;
    long configEndTimeTime = 0L;

    long initServerStartTime = System.currentTimeMillis();
    EnvUtils.buildCmdArgsMap(args);
    EnvUtils.load();
    // port and contextPath
    port = EnvUtils.getInt(ServerConfigKeys.SERVER_PORT, 80);
    String contextPath = EnvUtils.get(ServerConfigKeys.SERVER_CONTEXT_PATH);

    // httpConfig
    HttpConfig httpConfig = configHttp(port, contextPath);
    httpConfig.setBindIp(EnvUtils.get(ServerConfigKeys.SERVER_ADDRESS));

    // http request routes
    TioBootHttpControllerRouter controllerRouter = new TioBootHttpControllerRouter();
    tioBootServer.setControllerRouter(controllerRouter);

    // cacheFactory
    ConcurrentMapCacheFactory cacheFactory = ConcurrentMapCacheFactory.INSTANCE;

    // defaultHttpRequestHandlerDispather
    ITioHttpRequestHandler usedHttpRequestHandler = tioBootServer.getHttpRequestHandler();
    if (usedHttpRequestHandler == null) {
      DefaultHttpRequestHandler defaultHttpRequestHandler = new DefaultHttpRequestHandler();
      tioBootServer.setDefaultHttpRequestHandler(defaultHttpRequestHandler);
      usedHttpRequestHandler = defaultHttpRequestHandler;
    }

    // config websocket
    IWebSocketHandler defaultWebScoketHanlder = tioBootServer.getDefaultWebSocketHandlerDispather();
    if (defaultWebScoketHanlder == null) {
      defaultWebScoketHanlder = new DefaultWebSocketHandlerDispather();
      tioBootServer.setDefaultWebSocketHandlerDispather(defaultWebScoketHanlder);
    }
    WebsocketServerConfig wsServerConfig = new WebsocketServerConfig(port);

    // config tcp
    ServerAioHandler serverAioHandler = tioBootServer.getServerAioHandler();

    // serverHandler
    TioBootServerHandler serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig, usedHttpRequestHandler, serverAioHandler);

    // 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
    ServerAioListener externalServerListener = tioBootServer.getServerAioListener();
    ServerAioListener serverAioListener = new TioBootServerHandlerListener(externalServerListener);

    // 配置对象
    ServerTioConfig serverTioConfig = new ServerTioConfig("tio-boot");
    serverTioConfig.setServerAioListener(serverAioListener);
    serverTioConfig.setServerAioHandler(serverHandler);
    serverTioConfig.setCacheFactory(cacheFactory);
    serverTioConfig.setDefaultIpRemovalListenerWrapper();
    serverTioConfig.statOn = EnvUtils.getBoolean(ServerConfigKeys.SERVER_STATA_ENABLE, false);

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
    serverTioConfig.setHeartbeatTimeout(EnvUtils.getInt(ServerConfigKeys.SERVER_BEARTBEAT_TIMEOUT, 0));
    SnowflakeId snowflakeId = new SnowflakeId();
    serverTioConfig.setTioUuid(snowflakeId);
    serverTioConfig.setReadBufferSize(EnvUtils.getInt(ServerConfigKeys.SERVER_READ_BUFFER_SIZE, 1024 * 30));
    serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, usedHttpRequestHandler);

    // TioServer
    tioBootServer.init(serverTioConfig, wsServerConfig, httpConfig);

    // defaultHttpServerInterceptorDispather
    HttpRequestInterceptor defaultHttpServerInterceptorDispather = tioBootServer.getDefaultHttpRequestInterceptorDispatcher();

    if (defaultHttpServerInterceptorDispather == null) {
      defaultHttpServerInterceptorDispather = new DefaultHttpRequestInterceptorDispatcher();
      tioBootServer.setDefaultHttpRequestInterceptorDispatcher(defaultHttpServerInterceptorDispather);
    }

    // httpReqeustSimpleHandlerRoute
    HttpRequestRouter httpReqeustSimpleHandlerRouter = tioBootServer.getRequestRouter();
    if (httpReqeustSimpleHandlerRouter == null) {
      httpReqeustSimpleHandlerRouter = new DefaultHttpReqeustRouter();
      tioBootServer.setRequestRouter(httpReqeustSimpleHandlerRouter);
    }

    HttpRequestFunctionRouter usedRquestFunctionRouter = tioBootServer.getRequestFunctionRouter();
    if (usedRquestFunctionRouter == null) {
      usedRquestFunctionRouter = new DefaultHttpRequestFunctionRouter();
      tioBootServer.setRequestFunctionRouter(usedRquestFunctionRouter);
    }

    long initServerEndTime = System.currentTimeMillis();

    List<Class<?>> scannedClasses = null;
    boolean printScannedClasses = EnvUtils.getBoolean(ServerConfigKeys.AOP_PRINT_SCANNED_CLASSSES, false);
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

    if (scannedClasses != null && scannedClasses.size() > 0) {
      this.initAnnotation(scannedClasses);
    }

    if (tioBootConfiguration != null) {
      try {
        // Configure TioBootConfiguration
        tioBootConfiguration.config();
      } catch (IOException e) {
        throw new RuntimeException("Failed to configure TioBootConfiguration", e);
      }
    }

    HttpReqeustGroovyRouter httpReqeustGroovyRouter = tioBootServer.getReqeustGroovyRouter();
    RequestStatisticsHandler requestStatisticsHandler = tioBootServer.getRequestStatisticsHandler();
    ResponseStatisticsHandler responseStatisticsHandler = tioBootServer.getResponseStatisticsHandler();
    HttpRequestHandler forwardHandler = tioBootServer.getForwardHandler();
    HttpRequestHandler notFoundHandler = tioBootServer.getNotFoundHandler();

    if (usedHttpRequestHandler instanceof DefaultHttpRequestHandler) {
      ((DefaultHttpRequestHandler) usedHttpRequestHandler).init(httpConfig, cacheFactory,
          //
          defaultHttpServerInterceptorDispather,
          //
          httpReqeustSimpleHandlerRouter, httpReqeustGroovyRouter, usedRquestFunctionRouter,
          //
          controllerRouter,
          //
          forwardHandler, notFoundHandler,
          //
          requestStatisticsHandler, responseStatisticsHandler);
    }

    configEndTimeTime = System.currentTimeMillis();

    long serverStartTime = System.currentTimeMillis();

    ServerListener serverListener = tioBootServer.getTioBootServerListener();
    if (serverListener != null) {
      serverListener.boforeStart(primarySources, args);
    }

    // 根据参数判断是否启动服务器,默认启动服务器
    if (EnvUtils.getBoolean(ServerConfigKeys.SERVER_LISTENING_ENABLE, true)) {

      // start server
      try {
        serverTioConfig.init();
        tioBootServer.start(httpConfig.getBindIp(), httpConfig.getBindPort());
        ServerListener tioBootServerListener = tioBootServer.getTioBootServerListener();
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
    // 初始controller
    if (!EnvUtils.getBoolean(ServerConfigKeys.SERVER_LISTENING_ENABLE, false)) {
      if (controllerRouter != null) {
        ControllerFactory aopFactory = new AopControllerFactory();
        controllerRouter.addControllers(scannedClasses, aopFactory);
      }
    }
    long routeEndTime = System.currentTimeMillis();

    log.info("init:{}(ms),scan class:{}(ms),config:{}(ms),server:{}(ms),http route:{}(ms)", initServerEndTime - initServerStartTime, scanClassEndTime - scanClassStartTime,
        configEndTimeTime - configStartTime, serverEndTime - serverStartTime, routeEndTime - routeStartTime);

    if (!EnvUtils.getBoolean(ServerConfigKeys.SERVER_LISTENING_ENABLE, false)) {
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
    ServerListener serverListener = TioBootServer.me().getTioBootServerListener();
    try {
      if (serverListener != null) {
        serverListener.beforeStop();
      }
      TioBootServer.me().stop();
      TioThreadUtils.stop();
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

  private HttpConfig configHttp(int port, String contextPath) {
    // html/css/js等的根目录，支持classpath:，也支持绝对路径
    String pageRoot = EnvUtils.get(ServerConfigKeys.SERVER_RESOURCES_STATIC_LOCATIONS, "classpath:/pages");
    // httpConfig
    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (Exception e) {
      e.printStackTrace();
    }

    Integer maxLiveTimeOfStaticRes = EnvUtils.getInt(ServerConfigKeys.HTTP_MAX_LIVE_TIME_OF_STATIC_RES);
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
    httpConfig.setUseSession(EnvUtils.getBoolean(ServerConfigKeys.SERVER_SESSION_ENABLE, true));
    httpConfig.setCheckHost(EnvUtils.getBoolean(ServerConfigKeys.HTTP_CHECK_HOST, false));
    // httpMultipartMaxRequestZize
    Integer httpMultipartMaxRequestZize = EnvUtils.getInt(ServerConfigKeys.HTTP_MULTIPART_MAX_REQUEST_SIZE);
    if (httpMultipartMaxRequestZize != null) {
      httpConfig.setMaxLengthOfPostBody(httpMultipartMaxRequestZize);
    }
    // httpMultipartMaxFileZize
    Integer httpMultipartMaxFileZize = EnvUtils.getInt(ServerConfigKeys.HTTP_MULTIPART_MAX_FILE_ZIZE);
    if (httpMultipartMaxFileZize != null) {
      httpConfig.setMaxLengthOfMultiBody(httpMultipartMaxFileZize);
    }

    if (EnvUtils.getBoolean(ServerConfigKeys.HTTP_ENABLE_REQUEST_LIMIT, true)) {
      httpConfig.setSessionRateLimiter(new TioServerSessionRateLimiter());
    }
    return httpConfig;
  }

  @Override
  public int getPort() {
    return port;
  }
}