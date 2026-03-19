package com.litongjava.tio.http.server.intf;

import com.litongjava.tio.http.common.HttpRequest;

/**
 * @author tanyaowu
 *
 */
public interface CurrUseridGetter {

  /**
   * 根据HttpRequest获取当前用户的userid
   * @param request
   * @return
   */
  public String getUserid(HttpRequest request);

}
