package com.litongjava.tio.websocket.server;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.utils.thread.pool.SynThreadPoolExecutor;
import com.litongjava.tio.websocket.common.WebSocketSnowflakeId;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

/**
 *
 * @author tanyaowu 2017年7月30日 上午9:45:54
 */
public class WsServerStarter {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(WsServerStarter.class);
  private WebsocketServerConfig wsServerConfig = null;
  private IWebSocketHandler wsMsgHandler = null;
  private WebsocketServerAioHandler wsServerAioHandler = null;
  private WebSocketServerAioListener wsServerAioListener = null;
  private ServerTioConfig serverTioConfig = null;
  private TioServer tioServer = null;

  public TioServer getTioServer() {
    return tioServer;
  }

  /**
   * @return the wsServerConfig
   */
  public WebsocketServerConfig getWsServerConfig() {
    return wsServerConfig;
  }

  /**
   * @return the wsMsgHandler
   */
  public IWebSocketHandler getWsMsgHandler() {
    return wsMsgHandler;
  }

  /**
   * @return the wsServerAioHandler
   */
  public WebsocketServerAioHandler getWsServerAioHandler() {
    return wsServerAioHandler;
  }

  /**
   * @return the wsServerAioListener
   */
  public WebSocketServerAioListener getWsServerAioListener() {
    return wsServerAioListener;
  }

  /**
   * @return the serverTioConfig
   */
  public ServerTioConfig getServerTioConfig() {
    return serverTioConfig;
  }

  public WsServerStarter(int port, IWebSocketHandler wsMsgHandler) throws IOException {
    this(port, wsMsgHandler, null, null);
  }

  public WsServerStarter(int port, IWebSocketHandler wsMsgHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
    this(new WebsocketServerConfig(port), wsMsgHandler, tioExecutor, groupExecutor);
  }

  public WsServerStarter(WebsocketServerConfig wsServerConfig, IWebSocketHandler wsMsgHandler) throws IOException {
    this(wsServerConfig, wsMsgHandler, null, null);
  }

  public WsServerStarter(WebsocketServerConfig wsServerConfig, IWebSocketHandler wsMsgHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
    WebSocketSnowflakeId wsTioUuid = new WebSocketSnowflakeId();
    this.wsServerConfig = wsServerConfig;
    this.wsMsgHandler = wsMsgHandler;
    wsServerAioHandler = new WebsocketServerAioHandler(wsServerConfig, wsMsgHandler);
    wsServerAioListener = new WebSocketServerAioListener();
    serverTioConfig = new ServerTioConfig("Tio Websocket Server");
    serverTioConfig.setServerAioHandler(wsServerAioHandler);
    serverTioConfig.setServerAioListener(wsServerAioListener);
    serverTioConfig.setHeartbeatTimeout(0);
    serverTioConfig.setTioUuid(wsTioUuid);
    serverTioConfig.setReadBufferSize(1024 * 30);
    tioServer = new TioServer(serverTioConfig);

  }

  public void start() throws IOException {
    tioServer.start(wsServerConfig.getBindIp(), wsServerConfig.getBindPort());

  }
}
