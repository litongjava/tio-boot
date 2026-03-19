package com.litongjava.tio.http.server.handler;

import com.litongjava.model.type.TioTypeReference;

// 内部类，用于保存函数和类型引用
public class RouteEntry<R, T> {
  private final IHttpRequestFunction<R, T> function;
  private final TioTypeReference<T> typeReference;

  public RouteEntry(IHttpRequestFunction<R, T> function, TioTypeReference<T> typeReference) {
    this.function = function;
    this.typeReference = typeReference;
  }

  public IHttpRequestFunction<R, T> getFunction() {
    return function;
  }

  public TioTypeReference<T> getTypeReference() {
    return typeReference;
  }
}