package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.jfinal.aop.annotation.Import;
import com.litongjava.tio.boot.constatns.ConfigKeyConstants;
import com.litongjava.tio.boot.executor.Threads;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.http.handler.HttpRoutes;
import com.litongjava.tio.boot.http.handler.JFinalAopControllerFactory;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpServerInterceptor;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerListener;
import com.litongjava.tio.boot.websocket.handler.DefaultWebSocketHandler;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.http.server.mvc.intf.ControllerFactory;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCache;
import com.litongjava.tio.utils.thread.pool.SynThreadPoolExecutor;
import com.litongjava.tio.websocket.common.WsTioUuid;
import com.litongjava.tio.websocket.server.WsServerConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioApplicationContext implements Context {

  private TioBootServer tioBootServer;
  private StartupCallback beforeStart;
  private StartedCallBack afterStarted;
  private ShutdownCallback beforeStop;
  private ShutCallback afterStoped;

  @Override
  public Context run(Class<?>[] primarySources, String[] args) {
    return run(primarySources, null, args);
  }

  @Override
  public Context run(Class<?>[] primarySources, ServerAioHandler tcpHandler, String[] args) {
    return run(primarySources, tcpHandler, null, args);
  }

  /**
   * 1.先启动服务器
   * 2.初始配置类
   * 3.添加路由
   */
  @Override
  public Context run(Class<?>[] primarySources, ServerAioHandler tcpHandler, ServerAioListener listener,
      String[] args) {
    long serverStartTime = System.currentTimeMillis();
    Enviorment enviorment = new Enviorment(args);
    AopManager.me().addSingletonObject(enviorment);

    // 启动端口
    int port = enviorment.getInt(ConfigKeyConstants.http_port, 80);
    String contextPath = enviorment.get(ConfigKeyConstants.http_contexPath);
    HttpConfig httpConfig = configHttp(enviorment, port, contextPath);

    // 第二个参数也可以是数组,自动考试扫描handler的路径
    HttpRequestHandler requestHandler = null;
    DefaultHttpRequestHandler defaultHttpRequestHandler = null;
    HttpRoutes routes = null;
    try {
      requestHandler = AopManager.me().getAopFactory().getOnly(HttpRequestHandler.class);

      if (requestHandler == null) {
        routes = new HttpRoutes();
        Aop.put(HttpRoutes.class, routes);

        DefaultHttpServerInterceptor defaultHttpServerInterceptor = Aop.get(DefaultHttpServerInterceptor.class);

        defaultHttpRequestHandler = new DefaultHttpRequestHandler(httpConfig, routes, defaultHttpServerInterceptor);
        // requestHandler = new DefaultHttpRequestHandler(httpConfig, primarySources, jFinalAopControllerFactory);
        // requestHandler = new DefaultHttpRequestHandler(httpConfig, scannedClasses, jFinalAopControllerFactory);
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

    ServerAioHandler serverHandler = null;
    if (requestHandler != null) {
      serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig, requestHandler,
          tcpHandler);
    } else {
      serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig,
          defaultHttpRequestHandler, tcpHandler);
    }

    // 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
    ServerAioListener serverListener = new TioBootServerListener(listener);

    // 配置对象
    ServerTioConfig serverTioConfig = new ServerTioConfig("tio-boot", serverHandler, serverListener, tioExecutor,
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
    } catch (IOException e) {
      e.printStackTrace();
    }
    long serverEndTime = System.currentTimeMillis();

    long configStartTime = System.currentTimeMillis();

    List<Class<?>> scannedClasses = null;
    // 执行组件扫描
    try {
      scannedClasses = Aop.scan(primarySources);
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
    this.initAnnotation(scannedClasses);
    long configEndTimeTime = System.currentTimeMillis();

    long routeStartTime = System.currentTimeMillis();
    if (routes != null) {
      ControllerFactory jFinalAopControllerFactory = new JFinalAopControllerFactory();
      routes.addRoutes(scannedClasses, jFinalAopControllerFactory);
    }
    long routeEndTime = System.currentTimeMillis();
    log.info("server:{}(ms),config:{}(ms),http reoute:{}(ms)", serverEndTime - serverStartTime,
        configEndTimeTime - configStartTime, routeEndTime - routeStartTime);
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

  @Override
  public Context run(Class<?>[] primarySources, String[] args, StartupCallback startupCallback) {
    startupCallback.beforeStart(primarySources, args);
    return run(primarySources, args);
  }

  @Override
  public Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted, ShutdownCallback beforeStop, ShutCallback afterStoped) {
    this.beforeStart = beforeStart;
    this.afterStarted = afterStarted;
    this.beforeStop = beforeStop;
    this.afterStoped = afterStoped;
    beforeStart.beforeStart(primarySources, args);
    Context context = run(primarySources, args);
    afterStarted.afterStarted(primarySources, args);
    return context;
  }

  @Override
  public Context run(Class<?>[] primarySources, String[] args, ShutdownCallback beforeStop) {
    this.beforeStop = beforeStop;
    return run(primarySources, args);
  }

  @Override
  public Context run(Class<?>[] primarySources, String[] args, ShutdownCallback beforeStop, ShutCallback afterStoped) {
    this.beforeStop = beforeStop;
    this.afterStoped = afterStoped;
    return run(primarySources, args);
  }

  @Override
  public void initAnnotation(List<Class<?>> scannedClasses) {
    Aop.initAnnotation(scannedClasses);
  }

  @Override
  public void close() {
    log.info("stop server");
    try {
      if (beforeStop != null) {
        beforeStop.beforeStop();
      }
      tioBootServer.stop();
      Aop.close();
      if (afterStoped != null) {
        afterStoped.afterStoped();
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

  @Override
  public StartupCallback getBeforeStart() {
    return this.beforeStart;
  }

  @Override
  public StartedCallBack getAfterStarted() {
    return this.afterStarted;
  }

  @Override
  public ShutdownCallback getBeforeStop() {
    return this.beforeStop;
  }

  @Override
  public ShutCallback getafterStoped() {
    return this.afterStoped;
  }

  @Override
  public Context run(Class<?>[] primarySources, String[] args, ServerAioHandler tcpHandler, StartupCallback beforeStart,
      StartedCallBack afterStarted, ShutdownCallback beforeStop, ShutCallback afterStoped) {
    this.beforeStart = beforeStart;
    this.afterStarted = afterStarted;
    this.beforeStop = beforeStop;
    this.afterStoped = afterStoped;
    beforeStart.beforeStart(primarySources, args);
    Context context = run(primarySources, tcpHandler, args);
    afterStarted.afterStarted(primarySources, args);
    return context;
  }

  private HttpConfig configHttp(Enviorment enviorment, int port, String contextPath) {
    // html/css/js等的根目录，支持classpath:，也支持绝对路径
    String pageRoot = enviorment.get(ConfigKeyConstants.http_page, "classpath:/pages");
    // httpConfig
    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // maxLiveTimeOfStaticRes
    Integer maxLiveTimeOfStaticRes = enviorment.getInt(ConfigKeyConstants.http_maxLiveTimeOfStaticRes);
    String page404 = enviorment.get(ConfigKeyConstants.http_404);
    String page500 = enviorment.get(ConfigKeyConstants.http_500);
    if (maxLiveTimeOfStaticRes != null) {
      httpConfig.setMaxLiveTimeOfStaticRes(maxLiveTimeOfStaticRes);
    }
    Optional.ofNullable(page404).ifPresent((t) -> {
      httpConfig.setPage404(t);
    });
    Optional.ofNullable(page500).ifPresent((t) -> {
      httpConfig.setPage500(page500);
    });

    httpConfig.setUseSession(enviorment.getBoolean(ConfigKeyConstants.http_useSession, false));
    httpConfig.setCheckHost(enviorment.getBoolean(ConfigKeyConstants.http_checkHost, false));
    return httpConfig;
  }
}