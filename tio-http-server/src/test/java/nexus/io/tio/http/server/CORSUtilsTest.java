package nexus.io.tio.http.server;

import static org.junit.Assert.*;

import org.junit.Test;
import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.server.model.HttpCors;
import nexus.io.tio.http.server.util.CORSUtils;

public class CORSUtilsTest {
  private HttpRequest request(HttpMethod method, String origin) {
    HttpRequest request = new HttpRequest();
    request.requestLine = new RequestLine();
    request.requestLine.setMethod(method);
    request.requestLine.setPath("/api/user");
    request.addHeader("origin", origin);
    request.addHeader("access-control-request-method", "GET");
    request.addHeader("access-control-request-headers", "authorization, content-type");
    return request;
  }

  @Test
  public void preflightAllowsAuthorizationAndReflectsCredentialedOrigin() {
    HttpResponse response = CORSUtils.preflight(request(HttpMethod.OPTIONS, "https://cloud.example.com"));
    assertEquals(204, response.getStatus().status);
    assertEquals("https://cloud.example.com", response.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
    assertEquals("authorization, content-type", response.getHeader(HeaderName.Access_Control_Allow_Headers).toString());
  }

  @Test
  public void explicitRestrictionsArePreservedAndSharedConfigurationIsNotChanged() {
    HttpCors cors = new HttpCors();
    cors.setAllowOrigin("https://trusted.example.com");
    cors.setAllowHeaders("content-type");
    HttpResponse response = new HttpResponse(request(HttpMethod.OPTIONS, "https://other.example.com"));
    CORSUtils.enableCORS(response, cors);
    assertEquals("https://trusted.example.com", response.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
    assertEquals("content-type", response.getHeader(HeaderName.Access_Control_Allow_Headers).toString());
    HttpCors defaults = new HttpCors();
    CORSUtils.enableCORS(response, defaults);
    assertEquals("*", defaults.getAllowOrigin());
    assertEquals("*", defaults.getAllowHeaders());
    HttpResponse second = new HttpResponse(request(HttpMethod.OPTIONS, "https://second.example.com"));
    CORSUtils.enableCORS(second, defaults);
    assertEquals("https://second.example.com", second.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
  }

  @Test
  public void ordinaryRequestsAreNotPreflightsAndErrorResponsesReceiveCors() {
    HttpRequest get = request(HttpMethod.GET, "https://cloud.example.com");
    assertFalse(CORSUtils.isPreflightRequest(get));
    assertFalse(CORSUtils.isPreflightRequest(request(HttpMethod.OPTIONS, "")));
    HttpRequest options = new HttpRequest();
    options.requestLine = new RequestLine();
    options.requestLine.setMethod(HttpMethod.OPTIONS);
    options.addHeader("origin", "https://cloud.example.com");
    assertFalse(CORSUtils.isPreflightRequest(options));
    HttpResponse denied = new HttpResponse(get).setStatus(401);
    CORSUtils.enableCORS(denied);
    assertEquals(401, denied.getStatus().status);
    assertEquals("https://cloud.example.com", denied.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
  }

  @Test
  public void varyIsMergedWithoutLosingExistingFieldsOrDuplicatingEntries() {
    HttpResponse response = new HttpResponse(request(HttpMethod.OPTIONS, "https://cloud.example.com"));
    response.addHeader("vary", "Accept-Encoding, origin");
    CORSUtils.enableCORS(response);
    CORSUtils.enableCORS(response);
    assertEquals("Accept-Encoding, origin, Access-Control-Request-Method, Access-Control-Request-Headers",
        response.getHeader(HeaderName.Vary).toString());
    response.addHeader("vary", "*");
    CORSUtils.enableCORS(response);
    assertEquals("*", response.getHeader(HeaderName.Vary).toString());
  }

  @Test
  public void nonCredentialedWildcardRemainsWildcard() {
    HttpCors cors = new HttpCors();
    cors.setAllowCredentials("false");
    HttpResponse response = new HttpResponse(request(HttpMethod.GET, "https://cloud.example.com"));
    CORSUtils.enableCORS(response, cors);
    assertEquals("*", response.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
  }
}
