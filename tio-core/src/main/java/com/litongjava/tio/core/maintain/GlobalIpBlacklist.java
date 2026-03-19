package com.litongjava.tio.core.maintain;

import com.litongjava.tio.server.ServerTioConfig;

public enum GlobalIpBlacklist {
  INSTANCE;

  public IpBlacklist global = null;

  public void init(ServerTioConfig serverTioConfig) {
    if (serverTioConfig != null) {
      global = new IpBlacklist(serverTioConfig);
    }
  }
}
