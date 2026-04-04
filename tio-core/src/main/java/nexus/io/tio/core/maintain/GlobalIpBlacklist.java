package nexus.io.tio.core.maintain;

import nexus.io.tio.server.ServerTioConfig;

public enum GlobalIpBlacklist {
  INSTANCE;

  public IpBlacklist global = null;

  public void init(ServerTioConfig serverTioConfig) {
    if (serverTioConfig != null) {
      global = new IpBlacklist(serverTioConfig);
    }
  }
}
