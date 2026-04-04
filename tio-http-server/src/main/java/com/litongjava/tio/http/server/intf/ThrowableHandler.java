package com.litongjava.tio.http.server.intf;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.RequestLine;

/**
 * @author tanyaowu
 */
public interface ThrowableHandler {

  /**
   * 
   * @param request
   * @param requestLine
   * @param throwable
   * @return
   * @throws Exception 
   */
  public HttpResponse handler(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception;
}
