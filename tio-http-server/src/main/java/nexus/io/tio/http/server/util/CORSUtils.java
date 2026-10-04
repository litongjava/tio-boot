package nexus.io.tio.http.server.util;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.http.server.model.HttpCors;

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

    HttpRequest request = response.getHttpRequest();
    String allowOrigin = httpCors.getAllowOrigin();
    String allowCredentials = httpCors.getAllowCredentials();
    String allowHeaders = httpCors.getAllowHeaders();
    String allowMethods = httpCors.getAllowMethods();
    String exposeHeaders = httpCors.getExposeHeaders();
    String requestHeaders = httpCors.getRequestHeaders();
    String requestMethod = httpCors.getRequestMethod();
    String origin = httpCors.getOrigin();
    String maxAge = httpCors.getMaxAge();

    // Resolve wildcard defaults per response without mutating shared configuration.
    if (request != null) {
      if ("*".equals(allowOrigin) && "true".equalsIgnoreCase(allowCredentials)
          && StrUtil.isNotBlank(request.getOrigin())) {
        allowOrigin = request.getOrigin();
      }
      String requestedHeaders = request.getHeader("access-control-request-headers");
      if (isPreflightRequest(request) && "*".equals(allowHeaders) && StrUtil.isNotBlank(requestedHeaders)) {
        allowHeaders = requestedHeaders;
      }
    }

    response.addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from(allowOrigin));
    response.addHeader(HeaderName.Access_Control_Allow_Methods, HeaderValue.from(allowMethods));
    response.addHeader(HeaderName.Access_Control_Allow_Headers, HeaderValue.from(allowHeaders));
    response.addHeader(HeaderName.Access_Control_Max_Age, HeaderValue.from(maxAge));
    response.addHeader(HeaderName.Access_Control_Allow_Credentials, HeaderValue.from(allowCredentials));

    if (StrUtil.isNotBlank(exposeHeaders)) {
      response.addHeader(HeaderName.from("Access-Control-Expose-Headers"), HeaderValue.from(exposeHeaders));
    }

    if (StrUtil.isNotBlank(requestHeaders)) {
      response.addHeader(HeaderName.from("Access-Control-Request-Headers"), HeaderValue.from(requestHeaders));
    }

    if (StrUtil.isNotBlank(requestMethod)) {
      response.addHeader(HeaderName.from("Access-Control-Request-Method"), HeaderValue.from(requestMethod));
    }

    if (StrUtil.isNotBlank(origin)) {
      response.addHeader(HeaderName.Origin, HeaderValue.from(origin));
    }

    mergeVary(response, "Origin");
    mergeVary(response, "Access-Control-Request-Method");
    mergeVary(response, "Access-Control-Request-Headers");
  }
  /** Browser preflight declares the actual method rather than sending its credentials. */
  public static boolean isPreflightRequest(HttpRequest request) {
    return request != null && request.getRequestLine() != null
        && request.getRequestLine().getMethod() == HttpMethod.OPTIONS
        && StrUtil.isNotBlank(request.getOrigin())
        && StrUtil.isNotBlank(request.getHeader("access-control-request-method"));
  }

  /** Global CORS negotiation; the subsequent actual request still requires authentication. */
  public static HttpResponse preflight(HttpRequest request) {
    HttpResponse response = new HttpResponse(request);
    response.setStatus(204);
    enableCORS(response);
    return response;
  }

  private static void mergeVary(HttpResponse response, String name) {
    HeaderValue existing = response.getHeader(HeaderName.Vary);
    String value = existing == null ? "" : existing.toString();
    for (String item : value.split(",")) {
      if ("*".equals(item.trim()) || name.equalsIgnoreCase(item.trim())) {
        return;
      }
    }
    response.addHeader(HeaderName.Vary, HeaderValue.from(StrUtil.isBlank(value) ? name : value + ", " + name));
  }
}
