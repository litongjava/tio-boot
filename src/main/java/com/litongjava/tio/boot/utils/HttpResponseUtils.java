package com.litongjava.tio.boot.utils;

import com.jfinal.kit.StrKit;
import com.litongjava.tio.boot.model.HttpCors;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpResponse;

public class HttpResponseUtils {

  /**
   * enableCORS
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

    if (StrKit.notBlank(exposeHeaders)) {
      response.addHeader(HeaderName.from("Access-Control-Expose-Headers"), HeaderValue.from(exposeHeaders));
    }

    if (StrKit.notBlank(requestHeaders)) {
      response.addHeader(HeaderName.from("Access-Control-Request-Headers"), HeaderValue.from(requestHeaders));
    }

    if (StrKit.notBlank(requestMethod)) {
      response.addHeader(HeaderName.from("Access-Control-Request-Method"), HeaderValue.from(requestMethod));
    }

    if (StrKit.notBlank(origin)) {
      response.addHeader(HeaderName.from("Origin"), HeaderValue.from(origin));
    }
  }

}
