package nexus.io.tio.http.common;
import static org.junit.Assert.*;
import org.testng.annotations.Test;
import nexus.io.tio.http.common.utils.HttpParseUtils;
public class HeaderAttributeBoundaryTest {
  @Test public void namesMatchWholeParameters() {
    assertEquals("upload", HttpParseUtils.getSubAttribute("form-data; filename=photo.png; name=upload", "name"));
    assertNull(HttpParseUtils.getSubAttribute("form-data; filename=photo.png", "name"));
  }
  @Test public void quotedValuesPreserveSemicolonsAndEscapes() {
    assertEquals("a;b.txt", HttpParseUtils.getSubAttribute("form-data; filename=\"a;b.txt\"", "filename"));
    assertEquals("a\"b.txt", HttpParseUtils.getSubAttribute("form-data; filename=\"a\\\"b.txt\"", "filename"));
    assertEquals("", HttpParseUtils.getSubAttribute("form-data; name=\"\"", "name"));
  }
  @Test public void namesAreCaseInsensitiveAndWhitespaceIsAllowed() {
    assertEquals("AbCd", HttpParseUtils.getSubAttribute("multipart/form-data; BOUNDARY = AbCd ", "boundary"));
    assertEquals("UTF-8", HttpParseUtils.getSubAttribute("text/plain; CHARSET=\"UTF-8\"", "charset"));
  }
  @Test public void malformedQuotesAndMissingParametersReturnNull() {
    assertNull(HttpParseUtils.getSubAttribute("form-data; filename=\"unfinished", "filename"));
    assertNull(HttpParseUtils.getSubAttribute(null, "name"));
    assertNull(HttpParseUtils.getSubAttribute("text/plain", "charset"));
  }
}
