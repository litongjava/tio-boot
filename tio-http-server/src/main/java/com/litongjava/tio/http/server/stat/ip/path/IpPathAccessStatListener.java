package com.litongjava.tio.http.server.stat.ip.path;

import com.litongjava.tio.http.common.HttpRequest;

import nexus.io.tio.core.TioConfig;

public interface IpPathAccessStatListener {

  /**
   * 
   * @param tioConfig
   * @param ip
   * @param ipAccessStat
   * @author tanyaowu
   */
  public void onExpired(TioConfig tioConfig, String ip, IpAccessStat ipAccessStat);

  /**
   * 
   * @param httpRequest
   * @param ip
   * @param path
   * @param ipAccessStat
   * @param ipPathAccessStat
   * @author tanyaowu
   */
  public boolean onChanged(HttpRequest httpRequest, String ip, String path, IpAccessStat ipAccessStat,
      IpPathAccessStat ipPathAccessStat);

}
