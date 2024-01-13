package com.litongjava.tio.boot.server;

import java.io.IOException;

import com.litongjava.tio.boot.http.interceptor.ServerInteceptorConfigure;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.websocket.server.WsServerConfig;

public class TioBootServer {
  private static TioServer tioServer;
  private static WsServerConfig wsServerConfig;
  private static HttpConfig httpConfig;
  private static ServerInteceptorConfigure serverInteceptorConfigure = new ServerInteceptorConfigure();

  public static void init(ServerTioConfig serverTioConfig, WsServerConfig wsServerConfig, HttpConfig httpConfig) {
    // TODO Auto-generated method stub
    tioServer = new TioServer(serverTioConfig);
    TioBootServer.wsServerConfig = wsServerConfig;
    TioBootServer.httpConfig = httpConfig;
  }

  public static void start(String bindIp, Integer bindPort) throws IOException {
    tioServer.start(bindIp, bindPort);
  }

  public static boolean stop() {
    return tioServer.stop();
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
}
