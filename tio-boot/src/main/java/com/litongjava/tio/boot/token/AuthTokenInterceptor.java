package com.litongjava.tio.boot.token;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

/**
 * return null 表示验证通过
 */
public class AuthTokenInterceptor implements HttpRequestInterceptor {

  private TokenPredicate validateTokenLogic;

  public AuthTokenInterceptor(TokenPredicate validateTokenLogic) {
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
        PredicateResult validate = validateTokenLogic.validate(token);
        if (validate.isOk()) {
          String userId = validate.getUserId();
          TioRequestContext.setUserId(userId);
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
        PredicateResult validate = validateTokenLogic.validate(token);
        if (validate.isOk()) {
          String userId = validate.getUserId();
          TioRequestContext.setUserId(userId);
          return null;
        }
      }
    }
    HttpResponse response = TioRequestContext.getResponse();
    response.setStatus(HttpResponseStatus.C401);
    return response;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception {
  }
}