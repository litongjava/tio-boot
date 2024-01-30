package com.litongjava.tio.boot.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.litongjava.tio.boot.http.interceptor.DefaultHttpServerInterceptorDispatcher;
import com.litongjava.tio.boot.http.interceptor.ServerInteceptorConfigure;
import com.litongjava.tio.boot.http.routes.TioBootHttpRoutes;
import com.litongjava.tio.boot.tcp.ServerTcpHandler;
import com.litongjava.tio.boot.websocket.handler.WebSocketRoutes;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.handler.HttpRoutes;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.websocket.server.WsServerConfig;
import com.litongjava.tio.websocket.server.handler.IWsMsgHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioBootServer {
  private static TioServer tioServer;
  private static WsServerConfig wsServerConfig;
  private static HttpConfig httpConfig;
  private static ServerInteceptorConfigure serverInteceptorConfigure = new ServerInteceptorConfigure();
  private static WebSocketRoutes webSocketRoutes;
  private static HttpRequestHandler defaultHttpRequestHandlerDispather;
  private static DefaultHttpServerInterceptorDispatcher defaultHttpServerInterceptorDispatcher;
  private static IWsMsgHandler defaultWebSocketHandlerDispather;
  /**
   * close时执行的方法
   */
  private static List<Runnable> destroyMethods = new ArrayList<>();
  /**
   * 服务监听器
   */
  private static TioBootServerListener tioBootServerListener;

  /**
   * routes
   */
  private static TioBootHttpRoutes tioBootHttpRoutes;

  /**
   * httpRoutes
   */
  private static HttpRoutes httpRoutes;

  /**
   * ServerTcpHandler
   */
  private static ServerTcpHandler serverTcpHandler;
  /**
   * 
   */
  private static ServerAioListener serverAioListener;

  /**
   * @param serverTioConfig
   * @param wsServerConfig
   * @param httpConfig
   */
  public static void init(ServerTioConfig serverTioConfig, WsServerConfig wsServerConfig, HttpConfig httpConfig) {
    tioServer = new TioServer(serverTioConfig);
    TioBootServer.wsServerConfig = wsServerConfig;
    TioBootServer.httpConfig = httpConfig;
  }

  public static void start(String bindIp, Integer bindPort) throws IOException {
    tioServer.start(bindIp, bindPort);
  }

  /**
   * 关闭
   * @return
   */
  public static boolean stop() {
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

    return tioServer.stop();
  }

  public static boolean isRunning() {
    return tioServer != null;
  }

  public static TioServer getTioServer() {
    return tioServer;
  }

  public static WsServerConfig getWsServerConfig() {
    return wsServerConfig;
  }

  public static HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public static ServerInteceptorConfigure getServerInteceptorConfigure() {
    return serverInteceptorConfigure;
  }

  public static void setServerInteceptorConfigure(ServerInteceptorConfigure serverInteceptorConfigure) {
    TioBootServer.serverInteceptorConfigure = serverInteceptorConfigure;
  }

  public static void addDestroyMethod(Runnable runable) {
    destroyMethods.add(runable);
  }

  /**
   * 设置监听器
   * @param listener
   */
  public void setTioBootServerListener(TioBootServerListener listener) {
    TioBootServer.tioBootServerListener = listener;
  }

  public static TioBootServerListener getServerListener() {
    return tioBootServerListener;
  }

  /**
   * 设置tioBootHttpRoutes
   * @param tioBootHttpRoutes
   */
  public static void setTioBootHttpRoutes(TioBootHttpRoutes tioBootHttpRoutes) {
    TioBootServer.tioBootHttpRoutes = tioBootHttpRoutes;
  }

  public static TioBootHttpRoutes getTioBootHttpRoutes() {
    return tioBootHttpRoutes;
  }

  /**
   * 设置HttpRoutes
   */
  public static void setHttpRoutes(HttpRoutes httpRoutes) {
    TioBootServer.httpRoutes = httpRoutes;
  }

  public static HttpRoutes getHttpRoutes() {
    return httpRoutes;
  }

  /**
   * @param serverTcpHandler
   */
  public static void setServerTcpHandler(ServerTcpHandler serverTcpHandler) {
    TioBootServer.serverTcpHandler = serverTcpHandler;
  }

  public static ServerTcpHandler getServerTcpHandler() {
    return serverTcpHandler;
  }

  /**
   * @param serverAioListener
   */
  public static void setServerAioListener(ServerAioListener serverAioListener) {
    TioBootServer.serverAioListener = serverAioListener;
  }

  public static ServerAioListener getServerAioListener() {
    return serverAioListener;
  }

  public static WebSocketRoutes getWebSocketRoutes() {
    return webSocketRoutes;
  }

  public static void setWebSocketRoutes(WebSocketRoutes webSocketRoutes) {
    TioBootServer.webSocketRoutes = webSocketRoutes;

  }

  public static void setDefaultHttpServerInterceptorDispatcher(
      DefaultHttpServerInterceptorDispatcher defaultHttpServerInterceptorDispatcher) {
    TioBootServer.defaultHttpServerInterceptorDispatcher = defaultHttpServerInterceptorDispatcher;
  }

  public static DefaultHttpServerInterceptorDispatcher getDefaultHttpServerInterceptorDispatcher() {
    return defaultHttpServerInterceptorDispatcher;
  }

  public static void setDefaultHttpRequestHandlerDispather(HttpRequestHandler defaultHttpRequestHandlerDispather) {
    TioBootServer.defaultHttpRequestHandlerDispather = defaultHttpRequestHandlerDispather;
  }

  public static HttpRequestHandler getDefaultHttpRequestHandlerDispather() {
    return defaultHttpRequestHandlerDispather;
  }

  public static void setDefaultWebSocketHandlerDispather(IWsMsgHandler defaultWebSocketHandlerDispather) {
    TioBootServer.defaultWebSocketHandlerDispather = defaultWebSocketHandlerDispather;
  }

  public static IWsMsgHandler getDefaultWebSocketHandlerDispather() {
    return defaultWebSocketHandlerDispather;
  }
}
