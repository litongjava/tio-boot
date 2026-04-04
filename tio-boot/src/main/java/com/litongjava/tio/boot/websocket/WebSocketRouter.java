package com.litongjava.tio.boot.websocket;

import java.util.Map;

import nexus.io.tio.websocket.server.handler.IWebSocketHandler;

public interface WebSocketRouter {
  public IWebSocketHandler find(String path);

  public void add(String path, IWebSocketHandler wsHandler);
  
  public Map<String, IWebSocketHandler> all();

}
