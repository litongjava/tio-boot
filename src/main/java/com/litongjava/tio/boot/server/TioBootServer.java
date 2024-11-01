package com.litongjava.tio.boot.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.litongjava.context.ServerListener;
import com.litongjava.tio.boot.aspect.IGateWayCheckAspect;
import com.litongjava.tio.boot.aspect.IRequiresAuthenticationAspect;
import com.litongjava.tio.boot.aspect.IRequiresPermissionsAspect;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.handler.internal.RequestStatisticsHandler;
import com.litongjava.tio.boot.http.handler.internal.ResponseStatisticsHandler;
import com.litongjava.tio.boot.http.handler.internal.StaticResourceHandler;
import com.litongjava.tio.boot.http.interceptor.HttpInteceptorConfigure;
import com.litongjava.tio.boot.http.router.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.websocket.WebSocketRouter;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.router.HttpReqeustGroovyRouter;
import com.litongjava.tio.http.server.router.HttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.websocket.server.WebsocketServerConfig;
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
  private WebsocketServerConfig wsServerConfig;

  private ITioHttpRequestHandler httpRequestDispatcher;

  private HttpRequestInterceptor httpRequestInterceptorDispatcher;

  private IWebSocketHandler webSocketHandlerDispather;

  private HttpInteceptorConfigure httpInteceptorConfigure;
  private WebSocketRouter webSocketRouter;

  /**
   * 服务监听器
   */
  private ServerListener tioBootServerListener;

  /**
   * routes
   */
  private TioBootHttpControllerRouter controllerRouter;

  /**
   * httpRoutes
   */
  private HttpRequestRouter requestRouter;

  /**
   * httpReqeustGroovyRoute
   */
  private HttpReqeustGroovyRouter reqeustGroovyRouter;

  /**
   * 
   */
  private HttpRequestFunctionRouter requestFunctionRouter;

  /**
   * ServerTcpHandler
   */
  private ServerAioHandler serverAioHandler;
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

  private IGateWayCheckAspect gateWayCheckAspect;
  private IRequiresAuthenticationAspect requiresAuthenticationAspect;
  private IRequiresPermissionsAspect requiresPermissionsAspect;

  /**
   * Forward to other system
   */
  private HttpRequestHandler forwardHandler;

  /**
   * 
   */
  private StaticResourceHandler staticResourceHandler;
  /**
   * Not Found
   */
  private HttpRequestHandler notFoundHandler;

  /**
   * @param serverTioConfig
   * @param wsServerConfig
   * @param httpConfig
   */
  public void init(ServerTioConfig serverTioConfig, WebsocketServerConfig wsServerConfig, HttpConfig httpConfig) {
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
   * 
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

    boolean stop = tioServer.stop();
    me = new TioBootServer();
    return stop;
  }

  public boolean isRunning() {
    return tioServer != null;
  }

  public TioServer getTioServer() {
    return tioServer;
  }

  public WebsocketServerConfig getWsServerConfig() {
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
