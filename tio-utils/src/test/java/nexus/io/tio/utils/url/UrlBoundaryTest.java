package nexus.io.tio.utils.url;

import static org.junit.Assert.*;
import org.testng.annotations.Test;

public class UrlBoundaryTest {
  @Test
  public void preservesOpaqueUrlsAndEmptyPaths() {
    assertEquals("https://example.com", UrlUtils.encodeUrl("https://example.com"));
    assertEquals("mailto:user@example.com", UrlUtils.encodeUrl("mailto:user@example.com"));
    assertNull(UrlUtils.encodeUrl(null));
    assertNull(UrlUtils.decode(null));
  }

  @Test
  public void decodePreservesUnicodeAndLiteralPlus() {
    assertEquals("中文/😀+空 格", UrlUtils.decode("中文%2F😀+空%20格"));
    assertEquals("中文/😀+", UrlUtils.decode(UrlUtils.encode("中文/😀+")));
  }

  @Test
  public void encodeUrlPreservesEscapedSeparatorsAndPathSyntax() {
    String input = "https://example.com/a%2Fb/中文;x:y@z?q=%2F#part";
    String expected = "https://example.com/a%2Fb/%E4%B8%AD%E6%96%87;x:y@z?q=%2F#part";
    assertEquals(expected, UrlUtils.encodeUrl(input));
    assertEquals(expected, UrlUtils.encodeUrl(expected));
  }

  @Test
  public void rejectMalformedEscapes() {
    for (String value : new String[] {"%", "%1", "%GG", "%+1", "%-1"}) {
      try {
        UrlUtils.decode(value);
        fail(value);
      } catch (IllegalArgumentException expected) {
        // Each escape must contain exactly two hexadecimal digits.
      }
    }
  }
}
