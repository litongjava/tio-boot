package com.litongjava.tio.boot.swagger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class SwaggerResourceHandler {

  /**
   * 处理 /swagger-resources 请求
   *
   * @param request HTTP 请求
   * @return HTTP 响应，包含 Swagger 资源信息
   */
  public HttpResponse index(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();

    List<Map<String, Object>> resources = new ArrayList<>();

    Map<String, Object> resource = new HashMap<>();
    resource.put("name", "default");
    resource.put("url", "/v2/api-docs");
    resource.put("swaggerVersion", "2.0");
    resource.put("location", "/v2/api-docs");

    resources.add(resource);
    return response.setJson(resources);
  }

  /**
   * 处理 /swagger-resources/configuration/ui 请求
   *
   * @param request HTTP 请求
   * @return HTTP 响应，包含 Swagger UI 配置信息
   */
  public HttpResponse configurationUi(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();

    Map<String, Object> config = new LinkedHashMap<>();
    config.put("deepLinking", true);
    config.put("displayOperationId", false);
    config.put("defaultModelsExpandDepth", 1);
    config.put("defaultModelExpandDepth", 1);
    config.put("defaultModelRendering", "example");
    config.put("displayRequestDuration", false);
    config.put("docExpansion", "none");
    config.put("filter", false);
    config.put("operationsSorter", "alpha");
    config.put("showExtensions", false);
    config.put("tagsSorter", "alpha");
    config.put("validatorUrl", "");
    config.put("apisSorter", "alpha");
    config.put("jsonEditor", false);
    config.put("showRequestHeaders", false);
    config.put("supportedSubmitMethods", Arrays.asList("get", "put", "post", "delete", "options", "head", "patch", "trace"));

    return response.setJson(config);
  }
}
