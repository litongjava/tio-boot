package com.litongjava.tio.boot.mock;

import com.litongjava.tio.http.common.HttpMethod;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.RequestLine;

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
