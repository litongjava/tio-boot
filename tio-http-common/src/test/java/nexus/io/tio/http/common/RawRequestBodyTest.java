package nexus.io.tio.http.common;

import static org.testng.Assert.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;
import nexus.io.tio.http.common.HttpConst.RequestBodyFormat;
import nexus.io.tio.server.ServerChannelContext;
import nexus.io.tio.server.ServerTioConfig;
import nexus.io.tio.core.Node;

public class RawRequestBodyTest {
  @Test
  public void opaquePayloadIsPreservedWithoutFormDecoding() throws Exception {
    for (String type : new String[] {"application/octet-stream", "image/png", "application/x-protobuf"}) {
      byte[] body = new byte[] {0, (byte) 255, '%', 'Q', 'Z', '&', '=', 13, 10};
      String headers = "POST /binary HTTP/1.1\r\nHost: local\r\nContent-Type: " + type
          + "\r\nContent-Length: " + body.length + "\r\n\r\n";
      byte[] head = headers.getBytes(StandardCharsets.US_ASCII);
      ByteBuffer input = ByteBuffer.allocate(head.length + body.length);
      input.put(head).put(body).flip();
      ServerTioConfig server = new ServerTioConfig("raw-body-test");
      server.init();
      ServerChannelContext channel = new ServerChannelContext(server);
      channel.setClientNode(new Node("127.0.0.1", 12345));
      HttpRequest request = HttpRequestDecoder.decode(input, input.limit(), 0, input.remaining(), channel,
          new HttpConfig(0, false));
      assertEquals(request.getBodyFormat(), RequestBodyFormat.RAW);
      assertEquals(request.getBody(), body);
      assertTrue(request.getParams().isEmpty());
    }
  }
}
