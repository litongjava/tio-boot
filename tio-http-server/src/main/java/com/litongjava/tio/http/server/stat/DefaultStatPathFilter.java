package com.litongjava.tio.http.server.stat;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

/**
 * @author tanyaowu
 *
 */
public class DefaultStatPathFilter implements StatPathFilter {

  public static final DefaultStatPathFilter me = new DefaultStatPathFilter();

  /**
   * 
   */
  public DefaultStatPathFilter() {
  }

  @Override
  public boolean filter(String path, HttpRequest request, HttpResponse response) {
    return true;
  }

}
