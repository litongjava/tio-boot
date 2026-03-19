package com.litongjava.tio.http.server.intf;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;

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
