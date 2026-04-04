package nexus.io.tio.boot.swagger;

import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

public class SwaggerV2ApiDocsHandler {

  /**
   * 处理 /v2/api-docs 请求
   *
   * @param request
   * @return
   */
  public HttpResponse index(HttpRequest request) {
    TioSwaggerV2Config swaggerV2Config = TioBootServer.me().getSwaggerV2Config();
    HttpResponse response = TioRequestContext.getResponse();
    if (swaggerV2Config != null && swaggerV2Config.isEnable()) {
      response.setJson(swaggerV2Config.getSwaggerJson());
    } else {
      return response;
    }
    return response;
  }
}
