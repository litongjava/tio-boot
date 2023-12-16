package com.litongjava.tio.boot.server;

import java.io.IOException;

import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;

public class TioBootServer {
  private TioServer tioServer;

  public TioBootServer(ServerTioConfig serverTioConfig) {
    tioServer = new TioServer(serverTioConfig);

  }

  public void start(String bindIp, Integer bindPort) throws IOException {
    tioServer.start(bindIp, bindPort);
  }

  public void stop() {
    tioServer.stop();
  }

  public TioServer getTioServer() {
    return tioServer;
  }

}
