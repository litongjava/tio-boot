package com.litongjava.tio.boot.mock;

import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.RequestLine;

public class TioMock {
  public static HttpRequest mockGetHttpRequest() {
    // 模拟httpRequest
    RequestLine requestLine = new RequestLine();
    requestLine.setMethod(HttpMethod.GET);
    requestLine.setPath("/");
    requestLine.setVersion("1.1");
    HttpRequest request = new HttpRequest();

    request.setRequestLine(requestLine);
    return request;
  }

}
