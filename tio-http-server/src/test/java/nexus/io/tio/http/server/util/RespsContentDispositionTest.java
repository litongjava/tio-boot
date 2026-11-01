package nexus.io.tio.http.server.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HttpResponse;

public class RespsContentDispositionTest {

  @Test
  public void sendsAsciiFallbackAndUtf8FileName() {
    assertEquals("attachment; filename=\"__ 2026 a_b.xlsx\"; filename*=UTF-8''%E6%8A%A5%E8%A1%A8%202026%20a%22b.xlsx",
        Resps.attachmentDisposition("报表 2026 a\"b.xlsx"));
  }

  @Test
  public void plainAsciiNameIsKept() {
    assertEquals("attachment; filename=\"report.pdf\"; filename*=UTF-8''report.pdf",
        Resps.attachmentDisposition("report.pdf"));
  }

  @Test
  public void blankNameAddsNoHeader() {
    HttpResponse response = Resps.contentDisposition(new HttpResponse(), " ");
    assertNull(response.getHeader(HeaderName.Content_Disposition));
  }
}
