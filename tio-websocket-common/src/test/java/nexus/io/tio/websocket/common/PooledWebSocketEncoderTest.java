package nexus.io.tio.websocket.common;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.*;
import nexus.io.enhance.buffer.BufferPagePool;
import nexus.io.tio.core.pool.*;

public class PooledWebSocketEncoderTest {
  private BufferPagePool previous, pool;
  @Before public void setUp() { previous = BufferPoolUtils.bufferPool; pool = new BufferPagePool(1, false); BufferPoolUtils.bufferPool = pool; }
  @After public void tearDown() { BufferPoolUtils.bufferPool = previous; pool.release(); }
  @Test public void dirtyReusedStorageProducesExactFramesAcrossLengthBoundaries() {
    for (int size : new int[]{0, 1, 125, 126, 65535, 65536}) {
      byte[] body = new byte[size]; Arrays.fill(body, (byte) 37);
      WebSocketResponse packet = new WebSocketResponse(body); packet.setWsOpcode(Opcode.BINARY);
      ByteBuffer first;
      try (EncodedBuffer encoded = WebSocketServerEncoder.encodeBuffer(packet, null, null)) {
        first = encoded.buffer(); first.clear(); while (first.hasRemaining()) first.put((byte) 0xa5);
      }
      try (EncodedBuffer encoded = WebSocketServerEncoder.encodeBuffer(packet, null, null)) {
        ByteBuffer wire = encoded.buffer(); assertSame(first, wire); assertEquals(0x82, wire.get() & 255);
        int length = wire.get() & 127;
        long actual = length == 126 ? wire.getShort() & 65535 : length == 127 ? wire.getLong() : length;
        assertEquals(size, actual); assertEquals(size, wire.remaining());
        byte[] result = new byte[size]; wire.get(result); assertArrayEquals(body, result);
      }
    }
  }
  @Test public void clientMaskingPreservesPayloadAndContinuesAcrossParts() {
    byte[] first = new byte[]{1, 2, 3}, second = new byte[]{4, 5, 6, 7};
    WebSocketPacket packet = new WebSocketPacket(); packet.setBodys(new byte[][]{first, second}); packet.setWsEof(false);
    for (int attempt = 0; attempt < 2; attempt++) {
      try (EncodedBuffer encoded = WebSocketClientEncoder.encodeBuffer(packet, null, null)) {
        ByteBuffer wire = encoded.buffer(); assertEquals(2, wire.get() & 255); assertEquals(0x87, wire.get() & 255);
        byte[] mask = new byte[4]; wire.get(mask);
        for (int i = 0; i < 7; i++) assertEquals(i + 1, (wire.get() ^ mask[i & 3]) & 255);
        assertFalse(wire.hasRemaining());
      }
    }
    assertArrayEquals(new byte[]{1, 2, 3}, first); assertArrayEquals(new byte[]{4, 5, 6, 7}, second);
  }
  @Test public void clientSingleBodyIsReusableAcrossEncodes() {
    byte[] body = new byte[]{11, 22, 33}; WebSocketPacket packet = new WebSocketPacket(body); packet.setWsEof(true);
    for (int attempt = 0; attempt < 2; attempt++) {
      try (EncodedBuffer encoded = WebSocketClientEncoder.encodeBuffer(packet, null, null)) {
        ByteBuffer wire = encoded.buffer(); assertEquals(0x82, wire.get() & 255); assertEquals(0x83, wire.get() & 255);
        byte[] mask = new byte[4]; wire.get(mask);
        for (int i = 0; i < body.length; i++) assertEquals(body[i], (byte) (wire.get() ^ mask[i & 3]));
      }
    }
    assertArrayEquals(new byte[]{11, 22, 33}, body);
  }
}
