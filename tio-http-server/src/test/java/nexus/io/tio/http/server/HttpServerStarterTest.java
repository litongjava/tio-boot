package nexus.io.tio.http.server;

import java.io.IOException;

import nexus.io.model.body.RespBodyVo;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.handler.ITioHttpRequestHandler;
import nexus.io.tio.http.server.handler.DefaultHttpRequestDispatcher;
import nexus.io.tio.http.server.router.DefaultHttpRequestRouter;
import nexus.io.tio.http.server.router.HttpRequestRouter;
import nexus.io.tio.http.server.util.Resps;

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