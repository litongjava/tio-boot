package com.litongjava.tio.http.server;

import java.io.IOException;

import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.server.handler.DefaultHttpRequestDispatcher;
import com.litongjava.tio.http.server.router.DefaultHttpRequestRouter;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.tio.http.server.util.Resps;

public class HttpServerStarterTest {

  public static void main(String[] args) throws IOException {
    // 手动添加路由
    HttpServerStarterTest controller = new HttpServerStarterTest();

    HttpRequestRouter simpleHttpRoutes = new DefaultHttpRequestRouter();
    simpleHttpRoutes.add("/", controller::index);
    simpleHttpRoutes.add("/text", controller::text);
    simpleHttpRoutes.add("/exception", controller::exception);

    simpleHttpRoutes.add("/ok", (request) -> {
      return new HttpResponse(request).setJson(RespBodyVo.ok("ok"));
    });
    //
    HttpConfig httpConfig;
    ITioHttpRequestHandler requestHandler;
    HttpServerStarter httpServerStarter;

    // httpConfig
    httpConfig = new HttpConfig(80, null, null, null);

    requestHandler = new DefaultHttpRequestDispatcher(httpConfig, simpleHttpRoutes);
    httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
    httpServerStarter.start();
  }

  public HttpResponse index(HttpRequest request) {
    return Resps.txt(request, "index");

  }

  private HttpResponse text(HttpRequest request) {
    return Resps.txt(request, "login");
  }

  private HttpResponse exception(HttpRequest request) {
    throw new RuntimeException("error");
    // return Resps.txt(request, "exception");
  }

}