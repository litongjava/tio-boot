package nexus.io.tio.http.common;

import org.testng.annotations.Test;
import static org.junit.Assert.*;

public class HttpRequestBearerTokenTest {
  private HttpRequest request(String authorization) {
    HttpRequest request = new HttpRequest();
    if (authorization != null) { request.addHeader("authorization", authorization); }
    return request;
  }

  @Test public void stripsBearerPrefixWithoutChangingHeader() {
    HttpRequest request = request("Bearer example-token");
    assertEquals("example-token", request.getBearerToken());
    assertEquals("Bearer example-token", request.getAuthorization());
  }

  @Test public void preservesMissingEmptyAndRawValues() {
    assertNull(request(null).getBearerToken());
    assertEquals("", request("").getBearerToken());
    assertEquals("", request("Bearer ").getBearerToken());
    assertEquals("example-token", request("example-token").getBearerToken());
  }

  @Test public void usesExactPrefixAndDoesNotTrimOrValidate() {
    assertEquals("bearer example-token", request("bearer example-token").getBearerToken());
    assertEquals("Basic abc", request("Basic abc").getBearerToken());
    assertEquals(" example-token ", request("Bearer  example-token ").getBearerToken());
  }
}
