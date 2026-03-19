package com.litongjava.tio.http.server.intf;

import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.session.HttpSession;

/**
 * @author tanyaowu 
 * 2017年9月27日 下午1:46:20
 */
public interface HttpSessionListener {
  /**
   * 
   * @param request
   * @param session
   * @param httpConfig
   */
  public void doAfterCreated(HttpRequest request, HttpSession session, HttpConfig httpConfig);

}
