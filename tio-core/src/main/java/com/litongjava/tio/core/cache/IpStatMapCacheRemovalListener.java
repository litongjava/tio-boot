package com.litongjava.tio.core.cache;

import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.stat.IpStatListener;
import com.litongjava.tio.utils.cache.CacheRemovalListener;
import com.litongjava.tio.utils.cache.RemovalCause;

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