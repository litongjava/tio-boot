package com.litongjava.tio.boot.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.boot.http.handler.RequestStatisticsHandler;
import com.litongjava.tio.boot.http.handler.ResponseStatisticsHandler;
import com.litongjava.tio.boot.http.interceptor.ServerInteceptorConfigure;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoute;
import com.litongjava.tio.boot.tcp.ServerTcpHandler;
import com.litongjava.tio.boot.websocket.handler.WebSocketRoutes;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.router.HttpReqeustGroovyRoute;
import com.litongjava.tio.http.server.router.RequestRoute;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.websocket.server.WsServerConfig;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class TioBootServer {

  private static TioBootServer me = new TioBootServer();

  public static TioBootServer me() {
    return me;
  }

  private TioBootServer() {

  }

  private TioServer tioServer;
  private ServerTioConfig serverTioConfig;
  private HttpConfig httpConfig;
  private WsServerConfig wsServerConfig;

  private HttpRequestHandler httpRequestHandler;
  
  private DefaultHttpRequestHandler defaultHttpRequestHandler;

  private HttpRequestInterceptor defaultHttpRequestInterceptorDispatcher;

  private IWebSocketHandler defaultWebSocketHandlerDispather;

  private ServerInteceptorConfigure serverInteceptorConfigure;
  private WebSocketRoutes webSocketRoutes;

  /**
   * 服务监听器
   */
  private TioBootServerListener tioBootServerListener;

  /**
   * routes
   */
  private TioBootHttpControllerRoute tioBootHttpRoutes;

  /**
   * httpRoutes
   */
  private RequestRoute requestRoute;

  /**
   * dbRoutes
   */
  private HttpReqeustGroovyRoute httpReqeustGroovyRoute;

  /**
   * ServerTcpHandler
   */
  private ServerTcpHandler serverTcpHandler;
  /**
   * 
   */
  private ServerAioListener serverAioListener;

  /**
   * close时执行的方法
   */
  private List<Runnable> destroyMethods = new ArrayList<>();

  private RequestStatisticsHandler requestStatisticsHandler;
  
  private ResponseStatisticsHandler responseStatisticsHandler;

  private TioBootExceptionHandler exceptionHandler;

  /**
   * @param serverTioConfig
   * @param wsServerConfig
   * @param httpConfig
   */
  public void init(ServerTioConfig serverTioConfig, WsServerConfig wsServerConfig, HttpConfig httpConfig) {
    this.tioServer = new TioServer(serverTioConfig);
    this.serverTioConfig = serverTioConfig;
    this.wsServerConfig = wsServerConfig;
    this.httpConfig = httpConfig;
  }

  public void start(String bindIp, Integer bindPort) throws IOException {
    tioServer.start(bindIp, bindPort);
  }

  /**
   * 关闭
   * @return
   */
  public boolean stop() {
    Iterator<Runnable> iterator = destroyMethods.iterator();
    while (iterator.hasNext()) {
      Runnable runnable = iterator.next();
      iterator.remove();
      try {
        runnable.run();
      } catch (Exception e) {
        log.error("error occured while :{}", runnable);
      }
    }

    me = new TioBootServer();
    return tioServer.stop();
  }

  public boolean isRunning() {
    return tioServer != null;
  }

  public TioServer getTioServer() {
    return tioServer;
  }

  public WsServerConfig getWsServerConfig() {
    return wsServerConfig;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public static TioBootServer create() {
    return me;
  }

  public TioBootServer runOn() {
    return this;
  }

  public TioBootServer bindAddress(Object object) {
    return null;
  }

  public void addDestroyMethod(Runnable task) {
    destroyMethods.add(task);
  }
}
