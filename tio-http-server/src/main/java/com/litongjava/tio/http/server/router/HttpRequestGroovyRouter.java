package com.litongjava.tio.http.server.router;

import com.litongjava.tio.http.server.handler.HttpRequestHandler;

/**
 * 从数据库中加载路由
 * @author Tong Li
 *
 */
public interface HttpRequestGroovyRouter {

  /**
   * 添加路由
   * @param path
   * @param handler
   */
  public void add(String path, HttpRequestHandler handler);

  /**
   * 查找路由
   * @param path
   * @return
   */
  public HttpRequestHandler find(String path);
}
