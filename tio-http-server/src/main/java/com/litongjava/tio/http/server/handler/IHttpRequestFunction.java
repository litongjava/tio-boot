package com.litongjava.tio.http.server.handler;

@FunctionalInterface
public interface IHttpRequestFunction<R, T> {
  R handle(T t) throws Exception;
}
