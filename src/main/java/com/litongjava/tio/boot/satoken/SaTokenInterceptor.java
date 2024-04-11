package com.litongjava.tio.boot.satoken;

import com.litongjava.tio.boot.http.TioControllerContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpServerInterceptor;
import com.litongjava.tio.http.server.util.Resps;

import cn.dev33.satoken.stp.StpUtil;

public class SaTokenInterceptor implements HttpServerInterceptor {

  private Object body = null;

  public SaTokenInterceptor() {

  }

  public SaTokenInterceptor(Object body) {
    this.body = body;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    if (StpUtil.isLogin()) {
      return null;
    } else {
      HttpResponse response = TioControllerContext.getResponse();
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