package nexus.io.tio.http.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;

import nexus.io.tio.utils.json.JsonUtils;

public class HttpRequestMapTest {
  public static class Input {
    public String name;
    public String id;
  }

  private HttpRequest request(String rawBody) {
    HttpRequest request = new HttpRequest();
    request.setBodyString(rawBody);
    return request;
  }

  @Test
  public void requestMapOnlyReturnsDecodedParametersWithoutMutatingThem() {
    HttpRequest request = request("{\"id\":\"2\",\"name\":\"body\"}");
    request.addParam("id", "1");
    request.addParam("id", "3");
    Map<String, Object> data = request.getRequestMap();
    assertEquals("1", data.get("id"));
    assertFalse(data.containsKey("name"));
    data.put("id", "changed");
    assertEquals("1", request.getParam("id"));
    assertEquals(2, request.getParamArray("id").length);
  }

  @Test
  public void requestMapDoesNotParseAnyBody() {
    for (String body : new String[] { null, "", "{bad", "[]", "null", "1", "\"text\"", "{\"name\":\"body\"}" }) {
      HttpRequest request = request(body);
      request.addParam("name", "query");
      assertEquals("query", request.getRequestMap().get("name"));
    }
    assertTrue(request("{\"name\":\"body\"}").getRequestMap().isEmpty());
  }

  @Test
  public void formBodyUsesAlreadyDecodedParameters() {
    for (String contentType : new String[] { "application/x-www-form-urlencoded", "multipart/form-data" }) {
      HttpRequest request = request("name=hello%2Bworld");
      request.addHeader("content-type", contentType);
      request.addParam("name", "hello+world");
      assertEquals("hello+world", request.getRequestMap().get("name"));
    }
  }

  @Test
  public void bodyObjectParsesOnlyBodyAndPreservesLargeIdentifiers() {
    HttpRequest request = request("{\"id\":\"9223372036854775807\",\"name\":\"body\"}");
    request.addParam("name", "query");
    Input input = request.getBodyObject(Input.class);
    assertEquals("body", input.name);
    assertEquals("9223372036854775807", input.id);
    assertEquals("query", request.getParam("name"));
    assertNull(request.getRequestMap().get("id"));
  }

  @Test
  public void bodyObjectMatchesJsonUtilsForArraysAndNull() {
    HttpRequest request = request("[\"one\",\"two\"]");
    assertEquals("two", request.getBodyObject(String[].class)[1]);
    assertEquals(JsonUtils.parse("null", Input.class), request("null").getBodyObject(Input.class));
    assertEquals(JsonUtils.parse((String) null, Input.class), request(null).getBodyObject(Input.class));
  }

  @Test
  public void bodyObjectUsesCurrentBodyWithoutAnExtraSizeLimit() {
    String text = new String(new char[70000]).replace('\0', 'x');
    HttpRequest request = request("{\"name\":\"" + text + "\"}");
    assertEquals(text, request.getBodyObject(Input.class).name);
    request.setBodyString("{\"name\":\"updated\"}");
    assertEquals("updated", request.getBodyObject(Input.class).name);
  }
}
