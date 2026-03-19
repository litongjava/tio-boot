package com.litongjava.tio.http.server.handler;

import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.server.intf.ThrowableHandler;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.http.server.util.Resps;

public class DefaultHttpRequestDispatcher implements ITioHttpRequestHandler {

  private HttpRequestRouter httpRoutes;
  private HttpConfig httpConfig;
  private ThrowableHandler throwableHandler;

  public DefaultHttpRequestDispatcher(HttpConfig httpConfig, HttpRequestRouter httpRoutes) {
    this.httpRoutes = httpRoutes;
    this.httpConfig = httpConfig;
  }

  @Override
  public HttpResponse handler(HttpRequest httpRequest) throws Exception {
    RequestLine requestLine = httpRequest.getRequestLine();
    String path = requestLine.getPath();
    HttpRequestHandler handler = httpRoutes.find(path);
    if (handler == null) {
      return this.resp404(httpRequest, requestLine);

    }
    HttpResponse httpResponse = null;
    try {
      httpResponse = handler.handle(httpRequest);
    } catch (Exception e) {
      e.printStackTrace();
      return this.resp500(httpRequest, requestLine, e);
    }

    return httpResponse;
  }

  @Override
  public HttpResponse resp404(HttpRequest request, RequestLine requestLine) throws Exception {
    if (httpRoutes != null) {
      String page404 = httpConfig.getPage404();
      if (page404 != null) {
        HttpRequestHandler handler = httpRoutes.find(page404);
        if (handler != null) {
          return handler.handle(request);
        }
      }
    }

    return Resps.resp404(request, requestLine, httpConfig);

  }

  @Override
  public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {
    if (throwableHandler != null) {
      return throwableHandler.handler(request, requestLine, throwable);
    }

    if (httpRoutes != null) {
      String page500 = httpConfig.getPage500();
      if (page500 != null) {
        HttpRequestHandler handler = httpRoutes.find(page500);
        if (handler != null) {
          return handler.handle(request);
        }
      }

    }

    return Resps.resp500(request, requestLine, httpConfig, throwable);
  }

  @Override
  public HttpConfig getHttpConfig(HttpRequest request) {
    return request.getHttpConfig();
  }

  @Override
  public void clearStaticResCache() {

  }
}
