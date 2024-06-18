package com.litongjava.tio.boot.http.handler;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;

public interface ResponseStatisticsHandler {

  public void count(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse, long iv);

}
