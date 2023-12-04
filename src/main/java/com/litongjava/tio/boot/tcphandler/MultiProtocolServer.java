package com.litongjava.tio.boot.tcphandler;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.intf.TioUuid;
import org.tio.server.ServerTioConfig;
import org.tio.server.TioServer;
import org.tio.utils.Threads;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;
import org.tio.websocket.common.WsTioUuid;
import org.tio.websocket.server.handler.IWsMsgHandler;

/**
 * @author litongjava
 */
public class MultiProtocolServer {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(MultiProtocolServer.class);

  private MultiProcotolConfig protocolConfig = null;
  private MultiProtocolHandler protocolHandler = null;
  private MultiProtocolListener protocolListener = null;
  private ServerTioConfig serverTioConfig = null;
  private TioServer tioServer = null;

  public TioServer getTioServer() {
    return tioServer;
  }

  /**
   * @return the MultiProcotolConfig
   */
  public MultiProcotolConfig getMultiProcotolConfig() {
    return protocolConfig;
  }

  /**
   * @return the MultiProtocolHandler
   */
  public MultiProtocolHandler getMultiProtocolHandler() {
    return protocolHandler;
  }

  /**
   * @return the MultiProtocolListener
   */
  public MultiProtocolListener getMultiProtocolListener() {
    return protocolListener;
  }

  /**
   * @return the serverTioConfig
   */
  public ServerTioConfig getServerTioConfig() {
    return serverTioConfig;
  }

  public MultiProtocolServer(int port, IWsMsgHandler wsMsgHandler) throws IOException {
    this(port, wsMsgHandler, null, null);
  }

  public MultiProtocolServer(int port, IWsMsgHandler wsMsgHandler, SynThreadPoolExecutor tioExecutor,
      ThreadPoolExecutor groupExecutor) throws IOException {
    this(new MultiProcotolConfig(port), wsMsgHandler, tioExecutor, groupExecutor);
  }

  public MultiProtocolServer(MultiProcotolConfig MultiProcotolConfig, IWsMsgHandler wsMsgHandler) throws IOException {
    this(MultiProcotolConfig, wsMsgHandler, null, null);
  }

  public MultiProtocolServer(MultiProcotolConfig MultiProcotolConfig, IWsMsgHandler wsMsgHandler,
      SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
    this(MultiProcotolConfig, wsMsgHandler, new WsTioUuid(), tioExecutor, groupExecutor);
  }

  public MultiProtocolServer(MultiProcotolConfig MultiProcotolConfig, IWsMsgHandler wsMsgHandler, TioUuid tioUuid,
      SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
    if (tioExecutor == null) {
      tioExecutor = Threads.getTioExecutor();
    }

    if (groupExecutor == null) {
      groupExecutor = Threads.getGroupExecutor();
    }

    this.protocolConfig = MultiProcotolConfig;
    protocolHandler = new MultiProtocolHandler(MultiProcotolConfig);
    protocolListener = new MultiProtocolListener();
    serverTioConfig = new ServerTioConfig("Tio Server", protocolHandler, protocolListener, tioExecutor, groupExecutor);
    serverTioConfig.setHeartbeatTimeout(0);
    serverTioConfig.setTioUuid(tioUuid);
    serverTioConfig.setReadBufferSize(1024 * 30);
    tioServer = new TioServer(serverTioConfig);
  }

  public void start() throws IOException {
    tioServer.start(protocolConfig.getBindIp(), protocolConfig.getBindPort());

  }
}
