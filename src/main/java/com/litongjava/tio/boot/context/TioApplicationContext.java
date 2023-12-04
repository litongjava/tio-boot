package com.litongjava.tio.boot.context;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

import org.tio.http.common.HttpConfig;
import org.tio.http.common.TioConfigKey;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import org.tio.server.ServerTioConfig;
import org.tio.server.TioServer;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;
import org.tio.utils.cache.caffeine.CaffeineCache;
import org.tio.utils.jfinal.P;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;
import org.tio.websocket.common.WsTioUuid;
import org.tio.websocket.server.WsServerConfig;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.jfinal.aop.process.BeanProcess;
import com.litongjava.jfinal.aop.scaner.ComponentScanner;
import com.litongjava.tio.boot.constatns.ConfigKeyConstants;
import com.litongjava.tio.boot.executor.Threads;
import com.litongjava.tio.boot.httphandler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.httphandler.JFinalAopControllerFactory;
import com.litongjava.tio.boot.server.TioBootServerHandler;
import com.litongjava.tio.boot.server.TioBootServerListener;
import com.litongjava.tio.boot.websockethandler.DefaultWebSocketHandler;

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
    int port = enviorment.getInt(ConfigKeyConstants.http_port);
    String contextPath = enviorment.get(ConfigKeyConstants.http_contexPath);

    // html/css/js等的根目录，支持classpath:，也支持绝对路径
    String pageRoot = enviorment.get(ConfigKeyConstants.http_page);
    // maxLiveTimeOfStaticRes
    String page404 = enviorment.get(ConfigKeyConstants.http_404);
    String page500 = enviorment.get(ConfigKeyConstants.http_500);
    Integer maxLiveTimeOfStaticRes = P.getInt(ConfigKeyConstants.http_maxLiveTimeOfStaticRes);

    // httpConfig
    HttpConfig httpConfig = new HttpConfig(port, null, contextPath, null);

    try {
      httpConfig.setPageRoot(pageRoot);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (maxLiveTimeOfStaticRes != null) {
      httpConfig.setMaxLiveTimeOfStaticRes(maxLiveTimeOfStaticRes);
    }
    Optional.ofNullable(page404).ifPresent((t) -> {
      httpConfig.setPage404(t);
    });
    Optional.ofNullable(page500).ifPresent((t) -> {
      httpConfig.setPage500(page500);
    });

    httpConfig.setUseSession(P.getBoolean(ConfigKeyConstants.http_useSession, false));
    httpConfig.setCheckHost(P.getBoolean(ConfigKeyConstants.http_checkHost, false));

    // 第二个参数也可以是数组,自动考试扫描handler的路径
    HttpRequestHandler requestHandler = null;
    try {
      requestHandler = AopManager.me().getAopFactory().getOnly(HttpRequestHandler.class);

      if (requestHandler == null) {
        JFinalAopControllerFactory jFinalAopControllerFactory = new JFinalAopControllerFactory();
        requestHandler = new DefaultHttpRequestHandler(httpConfig, primarySources,jFinalAopControllerFactory);
      }
      // requestHandler=new BootHttpRequestHandler(httpConfig, primarySources);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // httpServerStarter
    // httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
    SynThreadPoolExecutor tioExecutor = Threads.newTioExecutor();
    ThreadPoolExecutor gruopExecutor = Threads.newGruopExecutor();

    // httpServerStarter = new HttpServerStarter(httpConfig, requestHandler, tioExecutor, gruopExecutor);

    // config websocket
    DefaultWebSocketHandler defaultWebScoketHanlder = new DefaultWebSocketHandler();
    WsServerConfig wsServerConfig = new WsServerConfig(port);

    ServerAioHandler serverHandler = new TioBootServerHandler(wsServerConfig, defaultWebScoketHanlder, httpConfig,
        requestHandler);
    // 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
    ServerAioListener serverListener = new TioBootServerListener();

    // 配置对象
    ServerTioConfig serverTioConfig = new ServerTioConfig(serverHandler, serverListener);

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
    tioServer.stop();
    Aop.close();
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