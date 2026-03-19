package com.litongjava.tio.boot.token;

import java.util.function.Predicate;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

public class UserTokenInterceptor implements HttpRequestInterceptor {

  private Object body = null;

  public UserTokenInterceptor() {

  }

  public UserTokenInterceptor(Object body) {
    this.body = body;
  }

  public UserTokenInterceptor(Object body, Predicate<String> validateTokenLogic) {
    this.body = body;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    Object userId = request.getUserId();
    if (userId == null) {
      HttpResponse response = TioRequestContext.getResponse();
      response.setStatus(HttpResponseStatus.C401);
      if (body != null) {
        response.setJson(body);
      }
      return response;
    }
    return null;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception {
  }
}