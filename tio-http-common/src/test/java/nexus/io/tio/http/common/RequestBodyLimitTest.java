package nexus.io.tio.http.common;

import java.nio.ByteBuffer;
import nexus.io.tio.server.ServerChannelContext;
import nexus.io.tio.server.ServerTioConfig;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import nexus.io.tio.core.exception.TioDecodeException;

public class RequestBodyLimitTest {
  private void rejected(String type, int length, int limit) throws Exception {
    HttpConfig config = new HttpConfig(0, false);
    config.setMaxLengthOfPostBody(1024);
    config.setMaxLengthOfRequestBody(limit);
    String headers = "POST / HTTP/1.1\r\nHost: local\r\nContent-Type: " + type
        + "\r\nContent-Length: " + length + "\r\n\r\n";
    ByteBuffer input = ByteBuffer.wrap(headers.getBytes(StandardCharsets.US_ASCII));
    try {
      HttpRequestDecoder.decode(input, input.limit(), 0, input.remaining(), null, config);
      fail("Oversized body accepted before receiving its payload");
    } catch (TioDecodeException expected) {
      assertTrue(expected.getMessage().contains("configured limit"));
    }
  }

  @Test public void checksRegularBodiesBeforeAllocation() throws Exception {
    rejected("application/json", 33, 32);
    rejected("application/x-www-form-urlencoded", 33, 32);
    rejected("text/plain", 33, 32);
    rejected("multipart/form-data-invalid", 33, 32);
  }

  @Test public void multipartAndDisabledLimitRetainTotalLimit() throws Exception {
    rejected("multipart/form-data; boundary=test", 1025, 32);
    rejected("application/json", 1025, 0);
  }

  @Test public void negativeLimitIsConfigurationError() {
    try {
      new HttpConfig(0, false).setMaxLengthOfRequestBody(-1);
      fail("Negative limit accepted");
    } catch (IllegalArgumentException expected) {
      assertNotNull(expected.getMessage());
    }
  }
  @Test public void allowsExactBoundaryAndIndependentMultipartBudget() throws Exception {
    String[] contentTypes = {"application/json", "Multipart/Form-Data; boundary=test", "application/json"};
    int[] limits = {32, 1, 0};
    for (int i = 0; i < contentTypes.length; i++) {
      HttpConfig config = new HttpConfig(0, false);
      config.setMaxLengthOfPostBody(1024);
      config.setMaxLengthOfRequestBody(limits[i]);
      String headers = "POST / HTTP/1.1\r\nHost: local\r\nContent-Type: " + contentTypes[i]
          + "\r\nContent-Length: 32\r\n\r\n";
      ByteBuffer input = ByteBuffer.wrap(headers.getBytes(StandardCharsets.US_ASCII));
      ServerChannelContext channel = new ServerChannelContext(new ServerTioConfig("body-limit-test"));
      assertNull(HttpRequestDecoder.decode(input, input.limit(), 0, input.remaining(), channel, config));
    }
  }
}
