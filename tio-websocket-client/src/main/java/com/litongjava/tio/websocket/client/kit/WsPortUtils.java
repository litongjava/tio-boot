package com.litongjava.tio.websocket.client.kit;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsPortUtils {

  private static final Logger log = LoggerFactory.getLogger(WsPortUtils.class);

  public static int getPort(String scheme, int port) {
    if (port == -1) {
      if (scheme.equals("ws")) {
        port = 80;
        log.info("No port specified, use the default: {}", port);
      } else {
        port = 443;
      }
    }
    return port;
  }

  public static int getPort(URI uri) {
    int port = uri.getPort();
    String scheme = uri.getScheme();
    if (port == -1) {
      if (scheme.equals("ws")) {
        port = 80;
        log.info("No port specified, use the default: {}", port);
      } else {
        port = 443;
      }
    }
    return port;
  }
}
