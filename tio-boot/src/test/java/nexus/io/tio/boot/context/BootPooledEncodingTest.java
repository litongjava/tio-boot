package nexus.io.tio.boot.context;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import org.junit.Test;
import nexus.io.aio.*;
import nexus.io.enhance.buffer.BufferPagePool;
import nexus.io.tio.boot.server.TioBootServerHandler;
import nexus.io.tio.core.*;
import nexus.io.tio.core.pool.*;
import nexus.io.tio.http.common.*;
import nexus.io.tio.http.server.HttpServerAioHandler;
import nexus.io.tio.server.intf.ServerAioHandler;
import nexus.io.tio.websocket.common.*;

public class BootPooledEncodingTest {
  @Test public void bootDispatchesHttpAndWebsocketToTheResponsePool() {
    BufferPagePool previous = BufferPoolUtils.bufferPool, pool = new BufferPagePool(1, false);
    BufferPoolUtils.bufferPool = pool;
    try {
      TioBootServerHandler handler = new TioBootServerHandler(null, null, null, null, null, null);
      try (EncodedBuffer result = handler.encodeBuffer(new HttpResponse().body("HTTP"), null, null)) {
        assertTrue(result.buffer().remaining() > 4);
      }
      long allocated = pool.getResponseBufferMemoryStat()[0].statNewAlloc;
      try (EncodedBuffer result = handler.encodeBuffer(new WebSocketResponse("WS"), null, null)) {
        assertEquals(4, result.buffer().remaining());
      }
      assertEquals(allocated, pool.getResponseBufferMemoryStat()[0].statNewAlloc);
      assertEquals(1, pool.getResponseBufferMemoryStat()[0].statReuseHit);
    } finally { BufferPoolUtils.bufferPool = previous; pool.release(); }
  }
  @Test public void customTcpEncoderCanTransferVirtualBufferOwnership() {
    final EncodedBuffer[] allocated = new EncodedBuffer[1];
    ServerAioHandler custom = new ServerAioHandler() {
      public Packet decode(ByteBuffer b,int l,int p,int r,ChannelContext x) { return null; }
      public ByteBuffer encode(Packet p,TioConfig c,ChannelContext x) { throw new AssertionError("Legacy encoding should not run"); }
      public EncodedBuffer encodeBuffer(Packet p,TioConfig c,ChannelContext x) {
        allocated[0] = EncodedBuffer.allocate(4); allocated[0].buffer().putInt(1234).flip(); return allocated[0];
      }
      public void handler(Packet p,ChannelContext x) {}
    };
    TioBootServerHandler handler = new TioBootServerHandler(null,null,null,null,custom,null);
    try (EncodedBuffer result = handler.encodeBuffer(new Packet(),null,null)) {
      assertSame(allocated[0],result); assertEquals(1234,result.buffer().getInt());
    }
  }
  @Test public void legacyHttpSubclassEncodingRemainsEffective() {
    HttpServerAioHandler handler = new HttpServerAioHandler(null,null) {
      @Override public ByteBuffer encode(Packet p,TioConfig c,ChannelContext x) { return ByteBuffer.wrap(new byte[]{42}); }
    };
    try (EncodedBuffer result = handler.encodeBuffer(new HttpResponse(),null,null)) { assertEquals(42,result.buffer().get()); }
  }
}
