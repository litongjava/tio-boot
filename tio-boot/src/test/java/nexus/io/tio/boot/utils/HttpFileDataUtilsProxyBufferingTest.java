package nexus.io.tio.boot.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.RandomAccessFile;

import org.junit.After;
import org.junit.Test;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpResponse;

public class HttpFileDataUtilsProxyBufferingTest {

  @After
  public void clear() {
    System.clearProperty(HttpFileDataUtils.DISABLE_PROXY_BUFFERING_KEY);
  }

  private static File largeFile() throws Exception {
    File file = File.createTempFile("zero-copy", ".mp4");
    file.deleteOnExit();
    try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
      raf.setLength(HttpFileDataUtils.ZERO_COPY_THRESHOLD + 1);
    }
    return file;
  }

  @Test
  public void zeroCopyFullAndRangeResponsesDisableProxyBuffering() throws Exception {
    File file = largeFile();
    HttpResponse full = HttpFileDataUtils.handleFullFileRequest(new HttpResponse(), file, file.length(), "video/mp4");
    assertEquals("no", value(full));

    HttpResponse range = HttpFileDataUtils.handleRangeRequest(new HttpResponse(), file, "bytes=0-", file.length(),
        "video/mp4");
    assertEquals(206, range.getStatus().status);
    assertEquals("no", value(range));
  }

  @Test
  public void canBeDisabled() throws Exception {
    System.setProperty(HttpFileDataUtils.DISABLE_PROXY_BUFFERING_KEY, "false");
    File file = largeFile();
    HttpResponse full = HttpFileDataUtils.handleFullFileRequest(new HttpResponse(), file, file.length(), "video/mp4");
    assertNull(full.getHeader(HeaderName.X_Accel_Buffering));
  }

  private static String value(HttpResponse response) {
    HeaderValue value = response.getHeader(HeaderName.X_Accel_Buffering);
    return value == null ? null : value.value;
  }
}
