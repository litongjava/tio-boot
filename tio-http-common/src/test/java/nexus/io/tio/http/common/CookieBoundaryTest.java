package nexus.io.tio.http.common;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.testng.annotations.Test;

public class CookieBoundaryTest {
  @Test
  public void emptyAndQuotedValuesArePreserved() {
    Map<String, String> values = Cookie.getEqualMap("empty=; quoted=\"\"; token=abc==; name=value");
    assertTrue(values.containsKey("empty"));
    assertEquals("", values.get("empty"));
    assertEquals("", values.get("quoted"));
    assertEquals("abc==", values.get("token"));
    assertEquals("value", values.get("name"));
  }

  @Test
  public void singleQuoteValueDoesNotBreakParsing() {
    assertEquals("\"", Cookie.getEqualMap("x=\"; next=ok").get("x"));
    assertEquals("ok", Cookie.getEqualMap("x=\"; next=ok").get("next"));
  }

  @Test
  public void requestRetainsEmptyCookie() {
    HttpRequest request = new HttpRequest();
    request.addHeader("cookie", "empty=; token=abc==");
    request.parseCookie(null);
    assertNotNull(request.getCookie("empty"));
    assertEquals("", request.getCookie("empty").getValue());
    assertEquals("abc==", request.getCookie("token").getValue());
  }

  @Test
  public void expirationIsIncludedInSerializedCookie() {
    Cookie cookie = new Cookie(null, "session", "", 0L);
    cookie.setExpires("Thu, 01 Jan 1970 00:00:00 GMT");
    assertTrue(cookie.toString().contains("; Expires=Thu, 01 Jan 1970 00:00:00 GMT"));
    assertTrue(cookie.toString().contains("; Max-Age=0"));
  }

  @Test
  public void mutationsInvalidateCachedEncoding() {
    Cookie cookie = new Cookie(null, "session", "old", 60L);
    Runnable[] changes = {() -> cookie.setName("other"), () -> cookie.setValue("new"),
        () -> cookie.setDomain("example.com"), () -> cookie.setPath("/app"),
        () -> cookie.setMaxAge(0L), () -> cookie.setExpires("Thu, 01 Jan 1970 00:00:00 GMT"),
        () -> cookie.setSecure(true), () -> cookie.setHttpOnly(true)};
    for (Runnable change : changes) {
      cookie.setBytes(cookie.toString().getBytes(StandardCharsets.UTF_8));
      change.run();
      assertNull(cookie.getBytes());
    }
  }

  @Test
  public void normalCookieAttributesRemainAvailable() {
    Cookie cookie = new Cookie("example.com", "session", "value", 60L);
    cookie.setSecure(true);
    cookie.setHttpOnly(true);
    String wire = cookie.toString();
    assertTrue(wire.startsWith("session=value"));
    assertTrue(wire.contains("Domain=example.com"));
    assertTrue(wire.contains("Path=/"));
    assertTrue(wire.contains("Secure"));
    assertTrue(wire.contains("httponly"));
  }
}
