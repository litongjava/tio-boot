package com.litongjava.tio.http.server.router;

import java.util.Map;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

public interface HttpRequestRouter {

  /**
   * 添加路由
   * 
   * @param path
   * @param handler
   */
  public void add(String path, HttpRequestHandler handler);

  /**
   * 查找路由
   * 
   * @param path
   * @return
   */
  public HttpRequestHandler find(String path);

  public Map<String, HttpRequestHandler> all();

  default HttpRequestHandler resolve(HttpRequest request) {
    return find(request.getRequestURI());
  }
}
