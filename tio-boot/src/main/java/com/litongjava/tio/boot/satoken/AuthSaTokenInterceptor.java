package com.litongjava.tio.boot.satoken;

import java.util.function.Predicate;

import com.litongjava.tio.boot.http.TioRequestContext;

import cn.dev33.satoken.stp.StpUtil;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.server.intf.HttpRequestInterceptor;
import nexus.io.tio.http.server.util.Resps;

public class AuthSaTokenInterceptor implements HttpRequestInterceptor {

  private Object body = null;

  private Predicate<String> validateTokenLogic;

  public AuthSaTokenInterceptor() {

  }

  public AuthSaTokenInterceptor(Object body) {
    this.body = body;
  }

  public AuthSaTokenInterceptor(Predicate<String> validateTokenLogic) {
    this.validateTokenLogic = validateTokenLogic;
  }

  public AuthSaTokenInterceptor(Object body, Predicate<String> validateTokenLogic) {
    this.body = body;
    this.validateTokenLogic = validateTokenLogic;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    if (validateTokenLogic != null) {
      String token = request.getHeader("token");
      if (token != null) {
        if (validateTokenLogic.test(token)) {
          return null;
        }
      }

      String authorization = request.getHeader("authorization");
      if (authorization != null) {
        String[] split = authorization.split(" ");
        if (split.length > 1) {
          if (validateTokenLogic.test(split[1])) {
            return null;
          }
        }
      }

    }

    if (StpUtil.isLogin()) {
      return null;
    } else {
      HttpResponse response = TioRequestContext.getResponse();
      response.setStatus(HttpResponseStatus.C401);
      if (body != null) {
        Resps.json(response, body);
      }
      return response;
    }
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost) throws Exception {
    // TODO Auto-generated method stub

  }

}