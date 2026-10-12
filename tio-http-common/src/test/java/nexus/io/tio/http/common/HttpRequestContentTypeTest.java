package nexus.io.tio.http.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.testng.annotations.Test;

public class HttpRequestContentTypeTest {
  @Test
  public void matchesMediaTypeWithoutChangingHeader() {
    HttpRequest request = new HttpRequest();
    request.addHeader("content-type", " Application/X-Www-Form-Urlencoded ; charset=UTF-8");
    assertTrue(request.isFormUrlEncoded());
    assertTrue(request.isContentType(" application/x-www-form-urlencoded "));
    assertFalse(request.isContentType("application/json"));
    assertEquals(" Application/X-Www-Form-Urlencoded ; charset=UTF-8", request.getContentType());
  }

  @Test
  public void rejectsAbsentEmptyAndDifferentTypes() {
    HttpRequest request = new HttpRequest();
    assertFalse(request.isFormUrlEncoded());
    for (String type : new String[] { "", " ", "; charset=UTF-8", "application/json",
        "multipart/form-data; boundary=test", "application/x-www-form-urlencoded-extra" }) {
      request.addHeader("content-type", type);
      assertFalse(request.isFormUrlEncoded());
    }
    assertFalse(request.isContentType(null));
    assertFalse(request.isContentType(" "));
  }

  @Test
  public void supportsOtherExactMediaTypes() {
    HttpRequest request = new HttpRequest();
    request.addHeader("content-type", "application/json; charset=UTF-8");
    assertTrue(request.isContentType("APPLICATION/JSON"));
    assertFalse(request.isContentType("application/*"));
    assertFalse(request.isContentType("application/json; charset=UTF-8"));
  }

  @Test
  public void identifiesJsonWithOptionalParameters() {
    HttpRequest request = new HttpRequest();
    assertFalse(request.isJson());
    for (String type : new String[] { "application/json", " Application/JSON ; charset=UTF-8" }) {
      request.addHeader("content-type", type);
      assertTrue(request.isJson());
      assertFalse(request.isFormUrlEncoded());
    }
    for (String type : new String[] { "", "text/json", "application/problem+json",
        "application/json-extra", "application/x-www-form-urlencoded" }) {
      request.addHeader("content-type", type);
      assertFalse(request.isJson());
    }
  }
}
