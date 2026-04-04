package com.litongjava.tio.boot.token;

import com.litongjava.tio.boot.http.TioRequestContext;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.server.intf.HttpRequestInterceptor;

public class UserTokenInterceptor implements HttpRequestInterceptor {

  private Object body = null;

  private TokenPredicate validateTokenLogic;

  public UserTokenInterceptor() {

  }

  public UserTokenInterceptor(TokenPredicate validateTokenLogic) {
    this.validateTokenLogic = validateTokenLogic;
  }

  public UserTokenInterceptor(Object body) {
    this.body = body;
  }

  public UserTokenInterceptor(TokenPredicate validateTokenLogic, Object body) {
    this.validateTokenLogic = validateTokenLogic;
    this.body = body;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    if (validateTokenLogic != null) {
      String token = GetTokenUtils.getToken(request);
      if (token != null) {
        PredicateResult validate = validateTokenLogic.validate(token);
        if (validate.isOk()) {
          String userId = validate.getUserId();
          if (userId != null) {
            request.setUserId(userId);
          }
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