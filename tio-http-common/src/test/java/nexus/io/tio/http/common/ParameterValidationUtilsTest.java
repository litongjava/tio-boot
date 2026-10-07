package nexus.io.tio.http.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.testng.annotations.Test;

import nexus.io.tio.http.common.utils.ParameterValidationUtils;
import nexus.io.model.exception.ParameterValidationException;

public class ParameterValidationUtilsTest {
  public static class Input {
    public String name;
    public int count;
  }

  private HttpRequest request(String body, String contentType) {
    HttpRequest request = new HttpRequest();
    request.setBodyString(body);
    request.addHeader("content-type", contentType);
    return request;
  }

  @Test
  public void jsonOverridesQueryAndLeavesOriginalParametersUnchanged() {
    HttpRequest request = request("{\"name\":\"body\",\"count\":2}", "application/json");
    request.addParam("name", "query");
    Input input = ParameterValidationUtils.body(request, Input.class);
    assertEquals("body", input.name);
    assertEquals(2, input.count);
    assertEquals("query", request.getParam("name"));
  }

  @Test
  public void bindsDecodedFormsMultipartAndQueryParameters() {
    for (String contentType : new String[] { "application/x-www-form-urlencoded", "multipart/form-data", "application/json" }) {
      HttpRequest request = request(null, contentType);
      request.addParam("name", "decoded");
      request.addParam("count", "3");
      assertEquals("decoded", ParameterValidationUtils.body(request, Input.class).name);
      assertEquals(3, ParameterValidationUtils.body(request, Input.class).count);
    }
    HttpRequest form = request("name=decoded", "application/x-www-form-urlencoded; charset=UTF-8");
    form.addParam("name", "decoded");
    assertEquals("decoded", ParameterValidationUtils.body(form, Input.class).name);
  }

  @Test
  public void rejectsMissingNonObjectMalformedAndIncompatibleParameters() {
    for (String body : new String[] { null, "", " ", "null", "[]", "1", "\"text\"", "{bad", "{\"count\":\"bad\"}" }) {
      try {
        ParameterValidationUtils.body(request(body, "application/json"), Input.class);
        fail("Accepted invalid parameters: " + body);
      } catch (ParameterValidationException expected) {
        // All invalid external parameters use the same public exception type.
      }
    }
  }
}
