package nexus.io.tio.http.common;

import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MultipartFilenameTest {
  @Test
  public void extendedUnicodeFilenameTakesPrecedence() throws Exception {
    HttpMultiBodyDecoder.Header header = new HttpMultiBodyDecoder.Header();
    HttpMultiBodyDecoder.parseHeader(Arrays.asList("Content-Disposition: form-data; name=\"file\"; filename=\"fallback.pdf\"; filename*=UTF-8''%E6%B3%95%E5%BE%8B+100%25.pdf", "Content-Type: application/pdf"), header, null);
    Assert.assertEquals(header.getFilename(), "法律+100%.pdf");
    Assert.assertEquals(header.getName(), "file");
  }

  @Test
  public void literalFilenamePreservesPercentAndColon() throws Exception {
    HttpMultiBodyDecoder.Header header = new HttpMultiBodyDecoder.Header();
    HttpMultiBodyDecoder.parseHeader(Arrays.asList("Content-Disposition: form-data; name=\"file\"; filename=\"100%:测试.pdf\""), header, null);
    Assert.assertEquals(header.getFilename(), "100%:测试.pdf");
  }
}
