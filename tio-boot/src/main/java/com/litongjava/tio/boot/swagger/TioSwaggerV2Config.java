package com.litongjava.tio.boot.swagger;

import springfox.documentation.service.ApiInfo;


public class TioSwaggerV2Config {
  private boolean isEnable;
  private ApiInfo apiInfo;
  // 预生成的 Swagger JSON
  private String swaggerJson;
  public TioSwaggerV2Config() {
    super();
    // TODO Auto-generated constructor stub
  }
  public TioSwaggerV2Config(boolean isEnable, ApiInfo apiInfo, String swaggerJson) {
    super();
    this.isEnable = isEnable;
    this.apiInfo = apiInfo;
    this.swaggerJson = swaggerJson;
  }
  public boolean isEnable() {
    return isEnable;
  }
  public void setEnable(boolean isEnable) {
    this.isEnable = isEnable;
  }
  public ApiInfo getApiInfo() {
    return apiInfo;
  }
  public void setApiInfo(ApiInfo apiInfo) {
    this.apiInfo = apiInfo;
  }
  public String getSwaggerJson() {
    return swaggerJson;
  }
  public void setSwaggerJson(String swaggerJson) {
    this.swaggerJson = swaggerJson;
  }
}
