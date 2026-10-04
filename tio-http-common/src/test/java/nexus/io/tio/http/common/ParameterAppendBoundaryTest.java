package nexus.io.tio.http.common;

import static org.junit.Assert.*;
import nexus.io.model.upload.UploadFile;
import org.testng.annotations.Test;

public class ParameterAppendBoundaryTest {
  @Test
  public void generalObjectIsRetainedOnFirstAndRepeatedAppend() {
    HttpRequest request = new HttpRequest();
    request.addParam("value", Integer.valueOf(42));
    assertEquals(Integer.valueOf(42), request.getObject("value"));
    request.addParam("value", Long.valueOf(43));
    assertArrayEquals(new Object[] {42, 43L}, request.getParamArray("value"));
  }

  @Test
  public void mixedFileAndTextPreserveOrderInBothDirections() {
    UploadFile file = new UploadFile("a.txt", new byte[] {1});
    for (Object[] values : new Object[][] {{file, "text"}, {"text", file}, {42, "text"}}) {
      HttpRequest request = new HttpRequest();
      request.addParam("value", values[0]);
      request.addParam("value", values[1]);
      assertArrayEquals(values, request.getParamArray("value"));
    }
  }

  @Test
  public void homogeneousValuesKeepTheirArrayTypes() {
    HttpRequest request = new HttpRequest();
    request.addParam("text", "one");
    request.addParam("text", "two");
    assertArrayEquals(new String[] {"one", "two"}, request.getParamValues("text"));
    UploadFile file = new UploadFile("a.txt", new byte[] {1});
    request.addParam("file", file);
    request.addParam("file", file);
    assertTrue(request.getParamArray("file") instanceof UploadFile[]);
    assertEquals(2, request.getParamArray("file").length);
  }

  @Test
  public void decodedTextCanFollowNonTextParameter() throws Exception {
    HttpRequest request = new HttpRequest();
    request.getParams().put("value", new Object[] {42});
    assertTrue(HttpRequestDecoder.decodeParams(request.getParams(), "value=text&value=next", "UTF-8", null));
    assertArrayEquals(new Object[] {42, "text", "next"}, request.getParamArray("value"));
  }

  @Test
  public void existingGenericStringArrayIsPromotedForStringAccess() {
    HttpRequest request = new HttpRequest();
    request.getParams().put("text", new Object[] {"one", null});
    request.addParam("text", "two");
    assertArrayEquals(new String[] {"one", null, "two"}, request.getParamValues("text"));
  }

  @Test
  public void nullAppendRemainsANoOp() {
    HttpRequest request = new HttpRequest();
    request.addParam("value", null);
    assertFalse(request.getParams().containsKey("value"));
    request.addParam("value", "one");
    request.addParam("value", null);
    assertArrayEquals(new String[] {"one"}, request.getParamValues("value"));
  }
}
