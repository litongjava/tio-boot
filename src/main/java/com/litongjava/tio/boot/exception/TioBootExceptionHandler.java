package com.litongjava.tio.boot.exception;

import com.litongjava.tio.http.common.HttpRequest;

public interface TioBootExceptionHandler {
  public void handler(HttpRequest request, Throwable e);
}
