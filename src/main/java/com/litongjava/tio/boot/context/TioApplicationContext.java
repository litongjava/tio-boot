package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.scaner.ComponentScanner;
import com.litongjava.tio.boot.constatns.ConfigKeyConstants;
import com.litongjava.tio.boot.executor.Threads;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.http.handler.HttpRoutes;
import com.litongjava.tio.boot.http.handler.JFinalAopControllerFactory;
import com.litongjava.tio.boot.http.interceptor.DefaultHttpServerInterceptor;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerListener;
import com.litongjava.tio.boot.websocket.handler.DefaultWebSocketHandler;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
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

  private TioServer tioServer;

  @Override
  public Context run(Class<?>[] primarySources, String[] args) {
    Enviorment enviorment = new Enviorment(args);
    AopManager.me().addSingletonObject(enviorment);

    List<Class<?>> scannedClasses = null;
    // 执行组件扫描
    try {
      scannedClasses = ComponentScanner.scan(primarySources);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    this.initAnnotation(scannedClasses);

    // 启动端口
    int port = enviorment.getInt(ConfigKeyConstants.http_port, 80);
    String contextPath = enviorment.get(ConfigKeyConstants.http_contexPath);
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

    // 第二个参数也可以是数组,自动考试扫描handler的路径
    HttpRequestHandler requestHandler = null;
    try {
      requestHandler = AopManager.me().getAopFactory().getOnly(HttpRequestHandler.class);

      if (requestHandler == null) {
        JFinalAopControllerFactory jFinalAopControllerFactory = new JFinalAopControllerFactory();
        HttpRoutes routes = new HttpRoutes(scannedClasses, jFinalAopControllerFactory);
        Aop.put(HttpRoutes.class, routes);

        DefaultHttpServerInterceptor defaultHttpServerInterceptor = Aop.get(DefaultHttpServerInterceptor.class);

        requestHandler = new DefaultHttpRequestHandler(httpConfig, routes, defaultHttpServerInterceptor);
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

//     httpServerStarter = new HttpServerStarter(httpConfig, requestHandler, tioExecutor, gruopExecutor);

    // config websocket
    DefaultWebSocketHandler defaultWebScoketHanlder = new DefaultWebSocketHandler();
    WsServerConfig wsServerConfig = new WsServerConfig(port);

    ServerAioHandler serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig,
        requestHandler);
    // 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
    ServerAioListener serverListener = new TioBootServerListener();

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
    serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, requestHandler);
    // TioServer对象
    tioServer = new TioServer(serverTioConfig);

    AopManager.me().addSingletonObject(tioServer);

    // 启动服务器
    try {
      tioServer.start(httpConfig.getBindIp(), httpConfig.getBindPort());
    } catch (IOException e) {
      e.printStackTrace();
    }
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
  public void close() {
    log.info("stop server");
    try {
      tioServer.stop();
      Aop.close();
    } catch (Exception e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @Override
  public boolean isRunning() {

    if (tioServer != null) {
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
    return tioServer;
  }

  @Override
  public void initAnnotation(List<Class<?>> scannedClasses) {
    BeanProcess beanProcess = new BeanProcess();
    beanProcess.initAnnotation(scannedClasses);
  }
}