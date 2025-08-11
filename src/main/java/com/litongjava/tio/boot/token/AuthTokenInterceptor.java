package com.litongjava.tio.boot.token;

import java.util.function.Predicate;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

public class AuthTokenInterceptor implements HttpRequestInterceptor {

  private Object body = null;

  private Predicate<String> validateTokenLogic;

  public AuthTokenInterceptor() {

  }

  public AuthTokenInterceptor(Object body) {
    this.body = body;
  }

  public AuthTokenInterceptor(Predicate<String> validateTokenLogic) {
    this.validateTokenLogic = validateTokenLogic;
  }

  public AuthTokenInterceptor(Object body, Predicate<String> validateTokenLogic) {
    this.body = body;
    this.validateTokenLogic = validateTokenLogic;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    if (validateTokenLogic != null) {
      String token = request.getParam("token");
      if (token == null) {
        token = request.getHeader("token");
      }
      if (token != null) {
        if (validateTokenLogic.test(token)) {
          return null;
        }
      }

      String authorization = request.getHeader("authorization");
      if (authorization != null) {
        String[] split = authorization.split(" ");

        if (split.length > 1) {
          token = split[1];
        } else {
          token = split[0];
        }
        if (validateTokenLogic.test(token)) {
          return null;
        }

      }
    }

    HttpResponse response = TioRequestContext.getResponse();
    response.setStatus(HttpResponseStatus.C401);
    if (body != null) {
      response.setJson(body);
    }
    return response;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception {
  }
}