package com.litongjava.tio.websocket.server;

import com.litongjava.tio.http.common.HttpConfig;

/**
 * @author tanyaowu
 * 2017年6月28日 下午2:42:59
 */
public class WebsocketServerConfig extends HttpConfig {

  public WebsocketServerConfig(Integer bindPort, boolean useSession) {
    super(bindPort, useSession);
  }

  public WebsocketServerConfig(Integer bindPort) {
    super(bindPort, true);
  }

}
