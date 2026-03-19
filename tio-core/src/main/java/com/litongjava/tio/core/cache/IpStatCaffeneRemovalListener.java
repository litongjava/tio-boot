package com.litongjava.tio.core.cache;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.stat.IpStatListener;

@SuppressWarnings("rawtypes")
public class IpStatCaffeneRemovalListener implements RemovalListener {
  private IpStatListener ipStatListener;
  private TioConfig tioConfig = null;

  /**
   * 
   * @author: tanyaowu
   */
  public IpStatCaffeneRemovalListener(TioConfig tioConfig, IpStatListener ipStatListener) {
    this.tioConfig = tioConfig;
    this.ipStatListener = ipStatListener;
  }

  // @Override
  // public void onRemoval(RemovalNotification notification) {
  // String ip = (String) notification.getKey();
  // IpStat ipStat = (IpStat) notification.getValue();
  //
  // if (ipStatListener != null) {
  // ipStatListener.onExpired(tioConfig, ipStat);
  // }
  //
  // // log.info("ip数据统计[{}]\r\n{}", ip, Json.toFormatedJson(ipStat));
  // }

  @Override
  public void onRemoval(Object key, Object value, RemovalCause cause) {
    // String ip = (String) key;
    IpStat ipStat = (IpStat) value;

    if (ipStatListener != null) {
      ipStatListener.onExpired(tioConfig, ipStat);
    }

  }
}
