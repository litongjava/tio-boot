package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.jfinal.aop.annotation.Import;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.process.BeforeStartConfigurationProcess;
import com.litongjava.jfinal.aop.process.ComponentAnnotation;
import com.litongjava.jfinal.aop.scaner.ComponentScanner;
import com.litongjava.tio.boot.constatns.ConfigKeys;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.http.handler.JFinalAopControllerFactory;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpServerInterceptor;
import com.litongjava.tio.boot.http.routes.TioBootHttpRoutes;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerHandlerListener;
import com.litongjava.tio.boot.server.TioBootServerListener;
import com.litongjava.tio.boot.tcp.ServerHanlderListener;
import com.litongjava.tio.boot.tcp.ServerTcpHandler;
import com.litongjava.tio.boot.websocket.handler.DefaultWebSocketHandler;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.http.server.annotation.RequestPath;
import com.litongjava.tio.http.server.handler.HttpRoutes;
import com.litongjava.tio.http.server.mvc.intf.ControllerFactory;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.Threads;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCache;
import com.litongjava.tio.utils.environment.EnvironmentUtils;
import com.litongjava.tio.utils.environment.PropUtils;
import com.litongjava.tio.utils.hutool.ResourceUtil;
import com.litongjava.tio.utils.thread.pool.SynThreadPoolExecutor;
import com.litongjava.tio.websocket.common.WsTioUuid;
import com.litongjava.tio.websocket.server.WsServerConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioApplicationContext implements Context {

  private TioBootServer tioBootServer;
  private TioBootServerListener serverListener;

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
    long serverStartTime = System.currentTimeMillis();
    EnvironmentUtils.buildCmdArgsMap(args);

    String env = EnvironmentUtils.get("app.env");
    if (ResourceUtil.getResource(ConfigKeys.defaultConfigFileName) != null) {
      PropUtils.use(ConfigKeys.defaultConfigFileName, env);
    } else {
      if (env != null) {
        PropUtils.use("app-" + env + ".properties");
      }
    }

    List<Class<?>> scannedClasses = null;
    // 添加自定义组件注解
    ComponentAnnotation.addComponentAnnotation(RequestPath.class);
    // 执行组件扫描
    try {
      scannedClasses = ComponentScanner.scan(primarySources);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    
    // 添加@Improt的类
    for (Class<?> primarySource : primarySources) {
      Import importAnnotaion = primarySource.getAnnotation(Import.class);
      if (importAnnotaion != null) {
        Class<?>[] value = importAnnotaion.value();
        for (Class<?> clazzz : value) {
          scannedClasses.add(clazzz);
        }
      }
    }
    scannedClasses = this.processBeforeStartConfiguration(scannedClasses);
    long scanClassEndTime = System.currentTimeMillis();
    serverListener = AopManager.me().getAopFactory().getOnly(TioBootServerListener.class);
    if (serverListener != null) {
      serverListener.boforeStart(primarySources, args);
    }
    // 启动端口
    int port = EnvironmentUtils.getInt(ConfigKeys.serverPort, 80);
    String contextPath = EnvironmentUtils.get(ConfigKeys.serverContextPath);
    HttpConfig httpConfig = configHttp(port, contextPath);
    httpConfig.setBindIp(EnvironmentUtils.get(ConfigKeys.serverAddress));

    // 第二个参数也可以是数组,自动考试扫描handler的路径
    HttpRequestHandler requestHandler = null;
    DefaultHttpRequestHandler defaultHttpRequestHandler = null;
    TioBootHttpRoutes routes = null;
    try {
      requestHandler = AopManager.me().getAopFactory().getOnly(HttpRequestHandler.class);

      if (requestHandler == null) {
        routes = new TioBootHttpRoutes();
        Aop.put(TioBootHttpRoutes.class, routes);

        DefaultHttpServerInterceptor defaultHttpServerInterceptor = Aop.get(DefaultHttpServerInterceptor.class);
        HttpRoutes httpRoutes = AopManager.me().getAopFactory().getOnly(HttpRoutes.class);
        defaultHttpRequestHandler = new DefaultHttpRequestHandler(httpConfig, routes, defaultHttpServerInterceptor,httpRoutes);
      }
      //
    } catch (Exception e) {
      e.printStackTrace();
    }

    // httpServerStarter
    // httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
    SynThreadPoolExecutor tioExecutor = Threads.newTioExecutor();
    ThreadPoolExecutor gruopExecutor = Threads.newGruopExecutor();

    // config websocket
    DefaultWebSocketHandler defaultWebScoketHanlder = new DefaultWebSocketHandler();
    WsServerConfig wsServerConfig = new WsServerConfig(port);

    ServerTcpHandler serverTcpHandler = AopManager.me().getAopFactory().getOnly(ServerTcpHandler.class);

    TioBootServerHandler serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig,
        defaultHttpRequestHandler, serverTcpHandler);

    // 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
    ServerAioListener externalServerListener = AopManager.me().getAopFactory().getOnly(ServerHanlderListener.class);
    ServerAioListener serverAioListener = new TioBootServerHandlerListener(externalServerListener);

    // 配置对象
    ServerTioConfig serverTioConfig = new ServerTioConfig("tio-boot", serverHandler, serverAioListener, tioExecutor,
        gruopExecutor);

    if (httpConfig.isUseSession()) {
      if (httpConfig.getSessionStore() == null) {
        CaffeineCache caffeineCache = CaffeineCache.register(httpConfig.getSessionCacheName(), null,
            httpConfig.getSessionTimeout());
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
    if (requestHandler != null) {
      serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, requestHandler);
    } else {
      serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, defaultHttpRequestHandler);
    }

    // TioServer对象
    tioBootServer = new TioBootServer(serverTioConfig);

    AopManager.me().addSingletonObject(tioBootServer);

    // 启动服务器
    try {
      tioBootServer.start(httpConfig.getBindIp(), httpConfig.getBindPort());
      if (serverListener != null) {
        serverListener.afterStarted(primarySources, args, this);
      }
    } catch (IOException e) {
      e.printStackTrace();
      this.close();
      System.exit(1);
    }
    long serverEndTime = System.currentTimeMillis();

    long configStartTime = System.currentTimeMillis();
    this.initAnnotation(scannedClasses);
    long configEndTimeTime = System.currentTimeMillis();

    long routeStartTime = System.currentTimeMillis();
    if (routes != null) {
      ControllerFactory jFinalAopControllerFactory = new JFinalAopControllerFactory();
      routes.addRoutes(scannedClasses, jFinalAopControllerFactory);
    }
    long routeEndTime = System.currentTimeMillis();
    log.info("scan class and init:{}(ms),server:{}(ms),config:{}(ms),http reoute:{}(ms)",
        scanClassEndTime - scanClassStartTime, serverEndTime - serverStartTime, configEndTimeTime - configStartTime,
        routeEndTime - routeStartTime);
    log.info("port:{}", port);
    String fullUrl = "http://localhost";
    if (port != 80) {
      fullUrl += (":" + port);
    }
    if (contextPath != null) {
      fullUrl += contextPath;
    }
    System.out.println(fullUrl);

    return this;
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
    try {
      if (serverListener != null) {
        serverListener.beforeStop();
      }
      tioBootServer.stop();
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
    if (tioBootServer != null) {
      return true;
    } else {
      return false;
    }

  }

  @Override
  public void restart(Class<?>[] primarySources, String[] args) {
    close();
    run(primarySources, args);
  }

  @Override
  public TioServer getServer() {
    return tioBootServer.getTioServer();
  }

  @Override
  public TioBootServer getTioBootServer() {
    return tioBootServer;
  }

  private HttpConfig configHttp(int port, String contextPath) {
    // html/css/js等的根目录，支持classpath:，也支持绝对路径
    String pageRoot = EnvironmentUtils.get(ConfigKeys.serverResourcesStaticLocations, "classpath:/pages");
    // httpConfig
    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // maxLiveTimeOfStaticRes
    Integer maxLiveTimeOfStaticRes = EnvironmentUtils.getInt(ConfigKeys.httpMaxLiveTimeOfStaticRes);
    String page404 = EnvironmentUtils.get(ConfigKeys.server404);
    String page500 = EnvironmentUtils.get(ConfigKeys.server500);
    if (maxLiveTimeOfStaticRes != null) {
      httpConfig.setMaxLiveTimeOfStaticRes(maxLiveTimeOfStaticRes);
    }
    Optional.ofNullable(page404).ifPresent((t) -> {
      httpConfig.setPage404(t);
    });
    Optional.ofNullable(page500).ifPresent((t) -> {
      httpConfig.setPage500(page500);
    });

    httpConfig.setUseSession(EnvironmentUtils.getBoolean(ConfigKeys.httpUseSession, false));
    httpConfig.setCheckHost(EnvironmentUtils.getBoolean(ConfigKeys.httpCheckHost, false));
    // httpMultipartMaxRequestZize
    Integer httpMultipartMaxRequestZize = EnvironmentUtils.getInt(ConfigKeys.httpMultipartMaxRequestZize);
    Optional.ofNullable(httpMultipartMaxRequestZize).ifPresent((t) -> {
      httpConfig.setMaxLengthOfPostBody(httpMultipartMaxRequestZize);
      log.info("set httpMultipartMaxRequestZize:{}", httpMultipartMaxRequestZize);
    });

    // httpMultipartMaxFileZize
    Integer httpMultipartMaxFileZize = EnvironmentUtils.getInt(ConfigKeys.httpMultipartMaxFileZize);
    Optional.ofNullable(httpMultipartMaxFileZize).ifPresent((t) -> {
      httpConfig.setMaxLengthOfMultiBody(httpMultipartMaxFileZize);
      log.info("set httpMultipartMaxFileZize:{}", httpMultipartMaxFileZize);
    });

    return httpConfig;
  }
}