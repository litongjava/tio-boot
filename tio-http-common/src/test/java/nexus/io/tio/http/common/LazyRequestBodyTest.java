package nexus.io.tio.http.common;

import static org.testng.Assert.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;
import nexus.io.tio.core.Node;
import nexus.io.tio.server.*;

public class LazyRequestBodyTest {
  @Test public void jsonStringIsCreatedOnDemandAndInvalidatedOnReplacement() throws Exception {
    String body = "{\"name\":\"hello\"}";
    byte[] wire = ("POST / HTTP/1.1\r\nHost: local\r\nContent-Type: application/json\r\nContent-Length: "
        + body.length() + "\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8);
    ServerTioConfig config = new ServerTioConfig("lazy-body"); config.init();
    ServerChannelContext channel = new ServerChannelContext(config); channel.setClientNode(new Node("127.0.0.1",1234));
    HttpRequest request = HttpRequestDecoder.decode(ByteBuffer.wrap(wire),wire.length,0,wire.length,channel,new HttpConfig(0,false));
    Field cache = HttpRequest.class.getDeclaredField("bodyString"); cache.setAccessible(true);
    assertNull(cache.get(request)); assertEquals(request.getBodyString(),body);
    assertSame(request.getBodyString(),request.getBodyString());
    request.setBodyBytes("other".getBytes(StandardCharsets.UTF_8)); assertEquals(request.getBodyString(),"other");
    request.setBody((byte[])null); assertNull(request.getBodyString());
    request.setBody((byte)'a'); assertEquals(request.getBodyString(),"a");
  }
  @Test public void directRequestLineParsingPreservesBodyAndRejectsOversizedLines() throws Exception {
    ByteBuffer direct = ByteBuffer.allocateDirect(1048576);
    byte[] line = "POST /tools/codex/v1/responses HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII);
    direct.position(5); direct.put(line); direct.put(new byte[90000]); direct.flip(); direct.position(5);
    RequestLine parsed = HttpRequestDecoder.parseRequestLine(direct,null);
    assertEquals(parsed.getPath(),"/tools/codex/v1/responses");
    assertEquals(direct.position(),5+line.length);
    direct.clear(); direct.put("GET /".getBytes(StandardCharsets.US_ASCII));
    for(int i=0;i<9000;i++) direct.put((byte)'a'); direct.flip();
    try { HttpRequestDecoder.parseRequestLine(direct,null); fail("Oversized direct request line accepted"); }
    catch(nexus.io.tio.core.exception.TioDecodeException expected) { assertTrue(expected.getMessage().contains("limit")); }
  }
  @Test public void incompleteLargeBodyDoesNotInflateSocketReadBuffer() throws Exception {
    ServerChannelContext channel = new ServerChannelContext(new ServerTioConfig("bounded-read"));
    channel.setReadBufferSize(1024);
    byte[] wire = "POST / HTTP/1.1\r\nHost: local\r\nContent-Length: 1048576\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    assertNull(HttpRequestDecoder.decode(ByteBuffer.wrap(wire),wire.length,0,wire.length,channel,new HttpConfig(0,false)));
    assertEquals(channel.getReadBufferSize().intValue(),1024);
    assertEquals(channel.packetNeededLength.intValue(),1048576 + wire.length);
  }
}
