package com.litongjava.tio.boot.satoken;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;
import com.litongjava.tio.http.server.util.Resps;

/**
 * 
 * @author 检查 http request header 的 Authorization 是指定的 token
 *
 */
public class FixedTokenInterceptor implements HttpRequestInterceptor {

  private Object body = null;
  private String authToken;

  public FixedTokenInterceptor(String authToken) {
    this.authToken = authToken;
  }

  public FixedTokenInterceptor(String authToken, Object body) {
    this.body = body;
    this.authToken = authToken;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    String authorization = request.getHeader("authorization");
    if (authorization == null) {
      authorization = request.getHeader("token");
      if (authorization == null) {
        authorization = request.getParam("authorization");
        if (authorization == null) {
          authorization = request.getParam("token");
        }
      }
    }
    if (authorization != null) {
      String[] array = authorization.split(" ");
      if (array.length > 1) {
        if (array[1].equals(authToken)) {
          return null;
        }
      } else {
        if (authorization.equals(authToken)) {
          return null;
        }
      }
    }

    HttpResponse response = TioRequestContext.getResponse();
    response.setStatus(HttpResponseStatus.C401);
    if (body != null) {
      Resps.json(response, body);
    }
    return response;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost) throws Exception {
    // TODO Auto-generated method stub

  }

}