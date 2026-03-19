package com.litongjava.tio.boot.http.handler.internal;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;

public interface ResponseStatisticsHandler {

  public void count(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse, Object userId,
      long elapsed);

}
