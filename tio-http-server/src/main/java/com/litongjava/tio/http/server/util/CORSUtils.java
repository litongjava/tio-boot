package com.litongjava.tio.http.server.util;

import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.model.HttpCors;

public class CORSUtils {

  public static void enableCORS(HttpResponse response) {
    CORSUtils.enableCORS(response, new HttpCors());
  }

  /**
   * enableCORS
   * 
   * @param response
   * @param httpCors
   */
  public static void enableCORS(HttpResponse response, HttpCors httpCors) {

    String allowOrigin = httpCors.getAllowOrigin();
    String allowCredentials = httpCors.getAllowCredentials();
    String allowHeaders = httpCors.getAllowHeaders();
    String allowMethods = httpCors.getAllowMethods();
    String exposeHeaders = httpCors.getExposeHeaders();
    String requestHeaders = httpCors.getRequestHeaders();
    String requestMethod = httpCors.getRequestMethod();
    String origin = httpCors.getOrigin();
    String maxAge = httpCors.getMaxAge();

    response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from(allowOrigin));
    response.addHeader(HeaderName.Access_Control_Allow_Methods, HeaderValue.from(allowMethods));
    response.addHeader(HeaderName.Access_Control_Allow_Headers, HeaderValue.from(allowHeaders));
    response.addHeader(HeaderName.Access_Control_Max_Age, HeaderValue.from(maxAge));
    response.addHeader(HeaderName.Access_Control_Allow_Credentials, HeaderValue.from(allowCredentials));

    if (exposeHeaders != null && exposeHeaders != "") {
      response.addHeader(HeaderName.from("Access-Control-Expose-Headers"), HeaderValue.from(exposeHeaders));
    }

    if (requestHeaders != null && requestHeaders != "") {
      response.addHeader(HeaderName.from("Access-Control-Request-Headers"), HeaderValue.from(requestHeaders));
    }

    if (requestMethod != null && requestMethod != "") {
      response.addHeader(HeaderName.from("Access-Control-Request-Method"), HeaderValue.from(requestMethod));
    }

    if (origin != null && origin != "") {
      response.addHeader(HeaderName.Origin, HeaderValue.from(origin));
    }

    response.addHeader(HeaderName.Vary, HeaderValue.from("Origin"));
    response.addHeader(HeaderName.Vary, HeaderValue.from("Access-Control-Request-Method"));
    response.addHeader(HeaderName.Vary, HeaderValue.from("Access-Control-Request-Headers"));
    response.addHeader(HeaderName.Keep_Alive, HeaderValue.from("timeout=60"));
    response.addHeader(HeaderName.Connection, HeaderValue.from("keep-alive"));
  }
}
