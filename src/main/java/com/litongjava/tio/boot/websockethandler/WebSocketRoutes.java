package com.litongjava.tio.boot.websockethandler;

import java.util.concurrent.ConcurrentHashMap;

import com.litongjava.tio.websocket.server.handler.IWsMsgHandler;

public class WebSocketRoutes {
  ConcurrentHashMap<String, Class<? extends IWsMsgHandler>> routes = new ConcurrentHashMap<>();

  public void add(String path, Class<? extends IWsMsgHandler> clazz) {
    routes.put(path, clazz);
  }

  public Class<? extends IWsMsgHandler> get(String path) {
    return routes.get(path);
  }
}
