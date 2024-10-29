package com.litongjava.tio.boot.http.handler;

import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.utils.cache.AbsCache;

public interface StaticResourceHandler {
  public HttpResponse handle(String path, HttpRequest request, HttpConfig httpConfig, AbsCache staticResCache);
}
