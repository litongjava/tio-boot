package com.litongjava.tio.boot.http;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class TioHttpAction {
  private HttpRequest request;
  private HttpResponse response;

  public TioHttpAction() {
    super();
  }

  public TioHttpAction(HttpRequest request, HttpResponse response) {
    super();
    this.request = request;
    this.response = response;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public void setRequest(HttpRequest request) {
    this.request = request;
  }

  public HttpResponse getResponse() {
    return response;
  }

  public void setResponse(HttpResponse response) {
    this.response = response;
  }

}
