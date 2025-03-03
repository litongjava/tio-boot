package com.litongjava.tio.boot.server;

import java.io.IOException;

import com.litongjava.context.ServerListener;
import com.litongjava.hook.HookCan;
import com.litongjava.tio.boot.aspect.IGateWayCheckAspect;
import com.litongjava.tio.boot.aspect.IRequiresAuthenticationAspect;
import com.litongjava.tio.boot.aspect.IRequiresPermissionsAspect;
import com.litongjava.tio.boot.decode.TioDecodeExceptionHandler;
import com.litongjava.tio.boot.email.EmailSender;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.http.handler.internal.RequestStatisticsHandler;
import com.litongjava.tio.boot.http.handler.internal.ResponseStatisticsHandler;
import com.litongjava.tio.boot.http.handler.internal.StaticResourceHandler;
import com.litongjava.tio.boot.http.interceptor.HttpInteceptorConfigure;
import com.litongjava.tio.boot.swagger.TioSwaggerV2Config;
import com.litongjava.tio.boot.websocket.WebSocketRouter;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.router.HttpRequestFunctionRouter;
import com.litongjava.tio.http.server.router.HttpRequestGroovyRouter;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.websocket.server.WebsocketServerConfig;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.Data;

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

  private IWebSocketHandler webSocketHandlerDispatcher;

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
  private HttpRequestGroovyRouter requestGroovyRouter;

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

  private RequestStatisticsHandler requestStatisticsHandler;

  private ResponseStatisticsHandler responseStatisticsHandler;

  private TioBootExceptionHandler exceptionHandler;

  private TioDecodeExceptionHandler decodeExceptionHandler;

  private IGateWayCheckAspect gateWayCheckAspect;
  private IRequiresAuthenticationAspect requiresAuthenticationAspect;
  private IRequiresPermissionsAspect requiresPermissionsAspect;

  /**
   * Forward to other system
   */
  private HttpRequestHandler forwardHandler;
  private StaticResourceHandler staticResourceHandler;
  /**
   * Not Found
   */
  private HttpRequestHandler notFoundHandler;

  private TioSwaggerV2Config swaggerV2Config;
  private EmailSender emailSender;

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
    boolean stop = tioServer.stop();
    HookCan.me().stop();
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
}
