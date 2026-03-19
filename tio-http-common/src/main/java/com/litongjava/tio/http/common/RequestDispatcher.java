package com.litongjava.tio.http.common;

public class RequestDispatcher {
  private String path;

  public RequestDispatcher(String path) {
    this.path = path;
  }

  public void forward(HttpRequest request, HttpResponse response) throws Exception {
    request.forward(path);
  }

}
