package com.litongjava.tio.boot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpCors {
  private String allowOrigin = "*";
  private String allowCredentials = "true";;
  private String allowHeaders = "Origin,X-Requested-With,Content-Type,Accept,Authorization,Jwt";
  private String allowMethods = "GET,PUT,POST,DELETE,PATCH,OPTIONS";
  private String exposeHeaders = "";
  private String requestHeaders = "";
  private String requestMethod = "";
  private String origin = "";
  private String maxAge = "3600";
}
