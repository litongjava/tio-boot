package nexus.io.tio.http.common;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class QueryParameterDecodingTest {
  @Test
  public void encodedNamesAndRepeatedNamesUseTheSameDecodedKey() throws Exception {
    HttpRequest request = new HttpRequest();
    assertTrue(HttpRequestDecoder.decodeParams(request.getParams(),
        "%69d=one&id=two&first+name=hello+world&%E5%90%8D=%E5%80%BC&x%3Dy=a%3Db", "UTF-8", null));
    assertEquals(request.getParams().get("id"), new String[] {"one", "two"});
    assertEquals(request.getParam("first name"), "hello world");
    assertEquals(request.getParam("名"), "值");
    assertEquals(request.getParam("x=y"), "a=b");
  }
}
