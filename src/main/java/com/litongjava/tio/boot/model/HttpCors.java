package com.litongjava.tio.boot.model;

import com.litongjava.tio.boot.annotation.EnableCORS;

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

  public HttpCors(EnableCORS enableCORS) {
    this.allowOrigin = enableCORS.allowOrigin();
    this.allowCredentials = enableCORS.allowCredentials();
    this.allowHeaders = enableCORS.allowHeaders();
    this.allowMethods = enableCORS.allowMethods();
    this.exposeHeaders = enableCORS.exposeHeaders();
    this.requestHeaders = enableCORS.requestHeaders();
    this.requestMethod = enableCORS.requestMethod();
    this.origin = enableCORS.origin();
    this.maxAge = enableCORS.maxAge();
  }
}
