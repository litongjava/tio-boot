package com.litongjava.tio.http.common;

/**
 * @author tanyaowu 2017年6月28日 下午2:23:16
 */
public enum HttpMethod {
  GET("GET"), POST("POST"), HEAD("HEAD"),
  //
  PUT("PUT"), DELETE("DELETE"),
  //
  TRACE("TRACE"), OPTIONS("OPTIONS"), PATCH("PATCH");

  public static HttpMethod from(String method) {
    if (method == null) {
      return null;
    }
    switch (method) {
    case "GET":
      return GET;
    case "POST":
      return POST;
    case "HEAD":
      return HEAD;
    case "PUT":
      return PUT;
    case "DELETE":
      return DELETE;
    case "TRACE":
      return TRACE;
    case "OPTIONS":
      return OPTIONS;
    case "PATCH":
      return PATCH;
    default:
      return null;
    }
  }

  String value;

  private HttpMethod(String value) {
    this.value = value;
  }
}
