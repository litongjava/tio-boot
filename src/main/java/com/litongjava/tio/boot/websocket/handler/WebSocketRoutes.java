package com.litongjava.tio.boot.websocket.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.litongjava.tio.websocket.server.handler.IWsMsgHandler;

public class WebSocketRoutes {
  ConcurrentHashMap<String, Class<? extends IWsMsgHandler>> routes = new ConcurrentHashMap<>();

  public void add(String path, Class<? extends IWsMsgHandler> clazz) {
    routes.put(path, clazz);
  }

  /**
   * /* 表示匹配任何以特定路径开始的路径，/** 表示匹配该路径及其下的任何子路径
   * @param path
   * @return
   */
  public Class<? extends IWsMsgHandler> get(String path) {
    // Direct match
    if (routes.containsKey(path)) {
      return routes.get(path);
    }

    // Check for wildcard matches
    for (Map.Entry<String, Class<? extends IWsMsgHandler>> entry : routes.entrySet()) {
      String key = entry.getKey();

      if (key.endsWith("/*")) {
        String baseRoute = key.substring(0, key.length() - 1);
        if (path.startsWith(baseRoute)) {
          return entry.getValue();
        }
      } else if (key.endsWith("/**")) {
        String baseRoute = key.substring(0, key.length() - 2);
        if (path.startsWith(baseRoute)) {
          return entry.getValue();
        }
      }
    }

    return null;
  }
}
