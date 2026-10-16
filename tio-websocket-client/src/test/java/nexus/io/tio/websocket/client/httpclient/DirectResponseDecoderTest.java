package nexus.io.tio.websocket.client.httpclient;

import static org.testng.Assert.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

public class DirectResponseDecoderTest {
  @Test public void parsesDirectAndSlicedHeapBuffersWithoutChangingPayload() throws Exception {
    byte[] message = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\npayload".getBytes(StandardCharsets.US_ASCII);
    for (boolean direct : new boolean[] {true,false}) {
      ByteBuffer base = direct ? ByteBuffer.allocateDirect(message.length+7) : ByteBuffer.allocate(message.length+7);
      base.position(7); base.put(message).flip(); base.position(7);
      ByteBuffer input = base.slice();
      assertNotNull(HttpResponseDecoder.parseResponseLine(input,null));
      Map<String,String> headers = new HashMap<>();
      assertTrue(HttpResponseDecoder.parseHeaderLine(input,headers,0));
      assertEquals(headers.get("upgrade"),"websocket");
      assertEquals(headers.get("connection"),"Upgrade");
      byte[] remaining = new byte[input.remaining()]; input.get(remaining);
      assertEquals(new String(remaining,StandardCharsets.US_ASCII),"payload");
    }
  }
  @Test public void fragmentedDirectResponseLineAndHeaderRemainIncomplete() throws Exception {
    ByteBuffer line = ByteBuffer.allocateDirect(64);
    line.put("HTTP/1.1 101 Switching".getBytes(StandardCharsets.US_ASCII)).flip();
    assertNull(HttpResponseDecoder.parseResponseLine(line,null));
    ByteBuffer header = ByteBuffer.allocateDirect(64);
    header.put("Upgrade: websock".getBytes(StandardCharsets.US_ASCII)).flip();
    assertFalse(HttpResponseDecoder.parseHeaderLine(header,new HashMap<String,String>(),0));
  }
}
