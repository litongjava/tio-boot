package nexus.io.tio.http.common;
import static org.junit.Assert.*;
import java.util.*;
import org.testng.annotations.Test;
public class RequestParsingAuditTest {
  @Test public void cookieNamesAreNotResponseAttributes() {
    HttpRequest request = new HttpRequest();
    request.addHeader("cookie", "path=abc; domain=example; max-age=text; secure=yes; httponly=no; expires=later");
    request.parseCookie(null);
    for (String name : new String[] {"path", "domain", "max-age", "secure", "httponly", "expires"}) {
      assertNotNull(name, request.getCookie(name));
      assertEquals(name, request.getCookie(name).getName());
    }
    assertEquals("text", request.getCookie("max-age").getValue());
  }
  @Test public void contentTypeIsIndependentOfDefaultLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      for (String type : new String[] {"APPLICATION/JSON", "application/json", "MULTIPART/FORM-DATA"}) {
        HttpRequest request = new HttpRequest();
        HttpRequestDecoder.parseBodyFormat(request, Collections.singletonMap("content-type", type));
        assertEquals(type.startsWith("MULTI") ? "MULTIPART" : "TEXT", request.getBodyFormat().toString());
      }
    } finally { Locale.setDefault(previous); }
  }
}
