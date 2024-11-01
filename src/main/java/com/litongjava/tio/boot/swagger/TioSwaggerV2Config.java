package com.litongjava.tio.boot.swagger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import springfox.documentation.service.ApiInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TioSwaggerV2Config {
  private boolean isEnable;
  private ApiInfo apiInfo;
  // 预生成的 Swagger JSON
  private String swaggerJson;
}
