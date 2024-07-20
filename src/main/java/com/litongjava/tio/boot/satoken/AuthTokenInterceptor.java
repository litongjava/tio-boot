package com.litongjava.tio.boot.satoken;

import java.util.function.Predicate;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.util.Resps;

import cn.dev33.satoken.stp.StpUtil;

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
      String token = request.getHeader("token");
      if (validateTokenLogic.test(token)) {
        return null;
      }

      String authorization = request.getHeader("authorization");

      if (validateTokenLogic.test(authorization)) {
        return null;
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
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception {
    // TODO Auto-generated method stub

  }

}