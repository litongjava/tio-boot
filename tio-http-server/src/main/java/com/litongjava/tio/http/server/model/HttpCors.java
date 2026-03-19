package com.litongjava.tio.http.server.model;

import com.litongjava.annotation.EnableCORS;

public class HttpCors {
  private String allowOrigin = "*";
  private String allowCredentials = "true";;
  private String allowHeaders = "*";
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

  public String getAllowOrigin() {
    return allowOrigin;
  }

  public void setAllowOrigin(String allowOrigin) {
    this.allowOrigin = allowOrigin;
  }

  public String getAllowCredentials() {
    return allowCredentials;
  }

  public void setAllowCredentials(String allowCredentials) {
    this.allowCredentials = allowCredentials;
  }

  public String getAllowHeaders() {
    return allowHeaders;
  }

  public void setAllowHeaders(String allowHeaders) {
    this.allowHeaders = allowHeaders;
  }

  public String getAllowMethods() {
    return allowMethods;
  }

  public void setAllowMethods(String allowMethods) {
    this.allowMethods = allowMethods;
  }

  public String getExposeHeaders() {
    return exposeHeaders;
  }

  public void setExposeHeaders(String exposeHeaders) {
    this.exposeHeaders = exposeHeaders;
  }

  public String getRequestHeaders() {
    return requestHeaders;
  }

  public void setRequestHeaders(String requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  public String getRequestMethod() {
    return requestMethod;
  }

  public void setRequestMethod(String requestMethod) {
    this.requestMethod = requestMethod;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(String maxAge) {
    this.maxAge = maxAge;
  }

  public HttpCors() {
    super();
  }

  public HttpCors(String allowOrigin, String allowCredentials, String allowHeaders, String allowMethods,
      String exposeHeaders, String requestHeaders, String requestMethod, String origin, String maxAge) {
    super();
    this.allowOrigin = allowOrigin;
    this.allowCredentials = allowCredentials;
    this.allowHeaders = allowHeaders;
    this.allowMethods = allowMethods;
    this.exposeHeaders = exposeHeaders;
    this.requestHeaders = requestHeaders;
    this.requestMethod = requestMethod;
    this.origin = origin;
    this.maxAge = maxAge;
  }


}
