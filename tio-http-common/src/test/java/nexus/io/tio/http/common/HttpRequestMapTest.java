package nexus.io.tio.http.common;

import java.util.Map;
import org.testng.annotations.Test;
import nexus.io.tio.utils.validator.ParameterValidationException;
import static org.junit.Assert.*;

public class HttpRequestMapTest {
  private HttpRequest request(final String rawBody) {
    return new HttpRequest() { @Override public String getBodyString() { return rawBody; } };
  }

  @Test public void jsonOverridesQueryWithoutMutatingOriginalParameters() {
    HttpRequest request = request("{\"id\":\"9223372036854775807\",\"payload\":{\"ok\":true}}");
    request.addParam("id", "1");
    Map<String, Object> data = request.getRequestMap();
    assertEquals("9223372036854775807", data.get("id"));
    assertTrue(data.get("payload") instanceof Map);
    assertEquals("1", request.getParam("id"));
  }

  @Test public void invalidJsonAndNonObjectsAreRejected() {
    for (String body : new String[] {"{bad", "[]", "null", "1", "\"text\""}) {
      try { request(body).getRequestMap(); fail("Accepted " + body); }
      catch (ParameterValidationException expected) { }
    }
  }

  @Test public void formBodyUsesAlreadyDecodedParameters() {
    HttpRequest request = request("name=hello");
    request.getHeaders().put("content-type", "application/x-www-form-urlencoded");
    request.addParam("name", "hello");
    assertEquals("hello", request.getRequestMap().get("name"));
  }

  @Test public void requestMapHasNoBusinessBodySizeLimit() {
    String text = new String(new char[70000]).replace('\0', 'x');
    assertEquals(text, request("{\"text\":\"" + text + "\"}").getRequestMap().get("text"));
  }
}
