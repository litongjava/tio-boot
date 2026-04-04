package nexus.io.tio.core.cache;

import nexus.io.tio.core.TioConfig;
import nexus.io.tio.core.stat.IpStat;
import nexus.io.tio.core.stat.IpStatListener;
import nexus.io.tio.utils.cache.CacheRemovalListener;
import nexus.io.tio.utils.cache.RemovalCause;

@SuppressWarnings("rawtypes")
public class IpStatMapCacheRemovalListener implements CacheRemovalListener {
  private IpStatListener ipStatListener;
  private TioConfig tioConfig = null;

  public IpStatMapCacheRemovalListener(TioConfig tioConfig, IpStatListener ipStatListener) {
    this.tioConfig = tioConfig;
    this.ipStatListener = ipStatListener;
  }

  @Override
  public void onCacheRemoval(Object key, Object value, RemovalCause cause) {
    IpStat ipStat = (IpStat) value;

    if (ipStatListener != null) {
      ipStatListener.onExpired(tioConfig, ipStat);
    }

  }

}