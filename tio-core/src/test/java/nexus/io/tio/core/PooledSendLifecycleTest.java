package nexus.io.tio.core;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import nexus.io.aio.Packet;
import nexus.io.enhance.buffer.*;
import nexus.io.tio.core.pool.*;
import nexus.io.tio.server.ServerChannelContext;

public class PooledSendLifecycleTest {
  private BufferPagePool previous;
  private BufferPagePool pool;
  @Before public void setUp() { previous = BufferPoolUtils.bufferPool; pool = new BufferPagePool(1, false); BufferPoolUtils.bufferPool = pool; }
  @After public void tearDown() { BufferPoolUtils.bufferPool = previous; pool.release(); }
  private int available() { return pool.getResponseBufferMemoryStat()[0].bufferSize; }
  private static class PooledEncoder extends CoreSendLifecycleTest.Encoder {
    ByteBuffer output;
    public EncodedBuffer encodeBuffer(Packet p, TioConfig config, ChannelContext context) {
      EncodedBuffer result = EncodedBuffer.allocate(3);
      output = result.buffer(); output.put(new byte[]{1, 2, 3}).flip();
      return result;
    }
  }
  @Test public void pendingAndPartialWritesKeepTheLeaseUntilCompletion() {
    CoreSendLifecycleTest.HoldingSink sink = new CoreSendLifecycleTest.HoldingSink();
    PooledEncoder encoder = new PooledEncoder(); ServerChannelContext x = CoreSendLifecycleTest.context(encoder, sink);
    try {
      assertTrue(Tio.send(x, CoreSendLifecycleTest.packet())); assertEquals(0, available());
      sink.pending.get();
      CompletionHandler<Integer,Object> callback = sink.completion; Object attachment = sink.attachment; sink.completion = null;
      callback.completed(1, attachment); assertEquals(0, available());
      sink.succeed(); assertEquals(1, available());
      try (EncodedBuffer next = EncodedBuffer.allocate(3)) { assertSame(encoder.output, next.buffer()); }
    } finally { Tio.close(x, "test cleanup"); }
  }
  @Test public void closeWaitsForThePendingIoCallbackBeforeReturningStorage() {
    CoreSendLifecycleTest.HoldingSink sink = new CoreSendLifecycleTest.HoldingSink() { public void close() { open = false; } };
    ServerChannelContext x = CoreSendLifecycleTest.context(new PooledEncoder(), sink);
    Tio.send(x, CoreSendLifecycleTest.packet()); Tio.close(x, "test close"); assertEquals(0, available());
    sink.completion.failed(new ClosedChannelException(), sink.attachment); assertEquals(1, available());
    sink.completion.failed(new ClosedChannelException(), sink.attachment); assertEquals(1, available());
  }
  @Test public void synchronousSubmissionFailureReturnsTheLease() {
    CoreSendLifecycleTest.Sink sink = new CoreSendLifecycleTest.Sink() {
      public <A> void write(ByteBuffer b, long t, TimeUnit u, A a, CompletionHandler<Integer,? super A> h) { throw new IllegalStateException("submission failed"); }
    };
    ServerChannelContext x = CoreSendLifecycleTest.context(new PooledEncoder(), sink);
    assertFalse(Tio.send(x, CoreSendLifecycleTest.packet())); assertEquals(1, available());
  }
  @Test public void allocationIsReturnedWhenEncodingThrows() {
    try {
      EncodedBuffer.encodePooled(allocator -> { allocator.apply(100); throw new IllegalArgumentException("encode failed"); });
      fail("Expected encoding failure");
    } catch (IllegalArgumentException expected) { assertEquals(1, available()); }
  }
  @Test public void ownerCloseIsIdempotentAndBorrowedBuffersStayOutsideThePool() {
    EncodedBuffer owner = EncodedBuffer.allocate(3); owner.close(); owner.close(); assertEquals(1, available());
    ByteBuffer external = ByteBuffer.wrap(new byte[]{1, 2, 3});
    try (EncodedBuffer borrowed = EncodedBuffer.borrowed(external)) { borrowed.buffer().get(); }
    assertEquals(0, external.position()); assertEquals(1, available());
  }
  @Test public void preEncodedPacketPreservesCallerBufferPositionAndOwnership() {
    ByteBuffer external = ByteBuffer.wrap(new byte[]{1, 2, 3});
    Packet packet = CoreSendLifecycleTest.packet(); packet.setPreEncodedByteBuffer(external);
    ServerChannelContext x = CoreSendLifecycleTest.context(new PooledEncoder(), new CoreSendLifecycleTest.Sink());
    try { assertTrue(Tio.send(x, packet)); assertEquals(0, external.position()); assertEquals(0, available()); }
    finally { Tio.close(x, "test cleanup"); }
  }
}
