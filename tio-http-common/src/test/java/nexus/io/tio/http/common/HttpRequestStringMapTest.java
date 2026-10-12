package nexus.io.tio.http.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;

import nexus.io.model.exception.ParameterValidationException;

public class HttpRequestStringMapTest {
  @Test
  public void preservesDecodedStringsAndReturnsIndependentCopy() throws Exception {
    HttpRequest request = new HttpRequest();
    assertTrue(HttpRequestDecoder.decodeParams(request.getParams(),
        "subject=%E7%A7%AF%E5%88%86+%252B&sign=a%2Bb%2Fc%3D&optional=", "UTF-8", null));
    Map<String, String> result = request.getRequestStringMap();
    assertEquals("积分 %2B", result.get("subject"));
    assertEquals("a+b/c=", result.get("sign"));
    assertEquals("", result.get("optional"));
    result.put("sign", "changed");
    assertEquals("a+b/c=", request.getParam("sign"));
  }

  @Test(expectedExceptions = ParameterValidationException.class)
  public void rejectsRepeatedDecodedNames() throws Exception {
    HttpRequest request = new HttpRequest();
    HttpRequestDecoder.decodeParams(request.getParams(), "amount=10&%61mount=20", "UTF-8", null);
    request.getRequestStringMap();
  }

  @Test(expectedExceptions = ParameterValidationException.class)
  public void rejectsNonStringObjects() {
    HttpRequest request = new HttpRequest();
    request.addParam("amount", Integer.valueOf(10));
    request.getRequestStringMap();
  }

  @Test
  public void doesNotParseJsonOrRequireParameters() {
    HttpRequest request = new HttpRequest();
    request.setBodyString("{\"amount\":10}");
    assertTrue(request.getRequestStringMap().isEmpty());
  }
}
