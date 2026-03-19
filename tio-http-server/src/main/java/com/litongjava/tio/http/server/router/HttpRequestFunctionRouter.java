
package com.litongjava.tio.http.server.router;

import com.litongjava.model.type.TioTypeReference;
import com.litongjava.tio.http.server.handler.IHttpRequestFunction;
import com.litongjava.tio.http.server.handler.RouteEntry;

public interface HttpRequestFunctionRouter {

  /**
   * 添加路由函数
   *
   * @param path          路由路径
   * @param function      IHttpRequestFunction 接口实现
   * @param typeReference 用于捕获函数的泛型类型
   */
  public <R, T> void add(String path, IHttpRequestFunction<R, T> function, TioTypeReference<T> typeReference);

  /**
   * 查找路由函数
   *
   * @param path 路径
   * @return RouteEntry 包含了 IHttpRequestFunction 和 TioTypeReference
   */
  public <R, T> RouteEntry<R, T> find(String path);
}
