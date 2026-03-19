package com.litongjava.tio.http.common.view;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

/**
 * @author tanyaowu
 *
 */
public interface View {
  /**
   * 
   * @param path 请求的路径
   * @param request
   * @return
   */
  public HttpResponse render(String path, HttpRequest request);
}
