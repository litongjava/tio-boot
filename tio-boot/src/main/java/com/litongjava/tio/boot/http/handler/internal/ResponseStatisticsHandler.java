package com.litongjava.tio.boot.http.handler.internal;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.RequestLine;

public interface ResponseStatisticsHandler {

  public void count(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse, Object userId,
      long elapsed);

}
