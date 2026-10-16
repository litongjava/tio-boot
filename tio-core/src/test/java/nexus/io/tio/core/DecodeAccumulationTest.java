package nexus.io.tio.core;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.Test;
import nexus.io.aio.Packet;
import nexus.io.enhance.buffer.VirtualBuffer;
import nexus.io.tio.core.task.DecodeTask;
import nexus.io.tio.server.*;
import nexus.io.tio.server.intf.ServerAioHandler;

public class DecodeAccumulationTest {
  static class Payload extends Packet {
    final byte[] bytes;
    Payload(byte[] bytes) { this.bytes = bytes; }
  }
  private ServerChannelContext context(List<byte[]> received, boolean needed) {
    ServerTioConfig config = new ServerTioConfig("accumulation-test");
    config.checkAttacks = false; config.ignoreDecodeFail = true;
    config.heartbeatTimeout = 0;
    config.setServerAioHandler(new ServerAioHandler() {
      public Packet decode(ByteBuffer b, int limit, int position, int readable, ChannelContext c) {
        if (readable < 4) return null;
        int length = b.getInt();
        if (length < 0) throw new IllegalArgumentException("invalid frame");
        if (b.remaining() < length) {
          if (needed) c.setPacketNeededLength(length + 4);
          return null;
        }
        byte[] bytes = new byte[length]; b.get(bytes); return new Payload(bytes);
      }
      public ByteBuffer encode(Packet p, TioConfig c, ChannelContext x) { return null; }
      public void handler(Packet p, ChannelContext c) { received.add(((Payload) p).bytes); }
    });
    config.init();
    ServerChannelContext c = new ServerChannelContext(config);
    c.setClientNode(new Node("127.0.0.1", 1234)); c.setClosed(false);
    new ReadCompletionHandler(c);
    return c;
  }
  private VirtualBuffer owned(DecodeTask task) throws Exception {
    Field field = DecodeTask.class.getDeclaredField("accumulation"); field.setAccessible(true);
    return (VirtualBuffer) field.get(task);
  }
  private byte[] frame(byte[] bytes) { return ByteBuffer.allocate(bytes.length + 4).putInt(bytes.length).put(bytes).array(); }

  @Test public void fragmentedFramesGrowGeometricallyAndOwnTheirBytes() throws Exception {
    for (boolean needed : new boolean[]{false, true}) {
      List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received, needed);
      byte[] body = new byte[65536]; new Random(7).nextBytes(body); byte[] wire = frame(body);
      Set<ByteBuffer> buffers = Collections.newSetFromMap(new IdentityHashMap<ByteBuffer, Boolean>());
      ByteBuffer input = ByteBuffer.allocate(73);
      for (int offset = 0; offset < wire.length;) {
        int count = Math.min(input.capacity(), wire.length - offset);
        input.clear(); input.put(wire, offset, count).flip(); c.decodeTask.decode(c, input);
        VirtualBuffer current = owned(c.decodeTask); if (current != null) buffers.add(current.buffer());
        Arrays.fill(input.array(), (byte) 0); offset += count;
      }
      assertEquals(1, received.size()); assertArrayEquals(body, received.get(0));
      assertTrue("allocations must be logarithmic, not one per read", buffers.size() <= 8);
      assertNull(owned(c.decodeTask));
    }
  }
  @Test public void coalescedFramesAndPartialTrailingFrameArePreserved() throws Exception {
    List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received, false);
    byte[] a = frame(new byte[]{1,2}), b = frame(new byte[]{3,4,5}), d = frame(new byte[]{6});
    c.decodeTask.decode(c, ByteBuffer.wrap(Arrays.copyOf(a, 2)));
    ByteBuffer rest = ByteBuffer.allocate(a.length - 2 + b.length + 3);
    rest.put(a, 2, a.length - 2).put(b).put(d,0,3).flip(); c.decodeTask.decode(c, rest);
    assertEquals(2, received.size());
    c.decodeTask.decode(c, ByteBuffer.wrap(d,3,d.length-3));
    assertEquals(3, received.size()); assertArrayEquals(new byte[]{6}, received.get(2)); assertNull(owned(c.decodeTask));
  }
  @Test public void errorAndConnectionCloseReleasePartialBuffer() throws Exception {
    List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received, false);
    c.decodeTask.decode(c, ByteBuffer.wrap(new byte[]{-1,-1})); assertNotNull(owned(c.decodeTask));
    c.decodeTask.decode(c, ByteBuffer.wrap(new byte[]{-1,-1})); assertNull(owned(c.decodeTask)); assertTrue(c.isClosed);
    c = context(received, true); c.decodeTask.decode(c, ByteBuffer.wrap(new byte[]{0,0}));
    assertNotNull(owned(c.decodeTask)); Tio.close(c, "test close"); assertNull(owned(c.decodeTask));
    c.decodeTask.close(); c.decodeTask.decode(c, ByteBuffer.wrap(new byte[]{0})); assertNull(owned(c.decodeTask));
  }
  @Test public void tinyTrailingPartialFrameDoesNotPinLargeBuffer() throws Exception {
    List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received, true);
    byte[] wire = frame(new byte[100000]);
    c.decodeTask.decode(c, ByteBuffer.wrap(wire,0,50000));
    ByteBuffer second = ByteBuffer.allocate(50006).put(wire,50000,50004).put((byte) 0).put((byte) 0);
    second.flip(); c.decodeTask.decode(c,second);
    assertEquals(1,received.size()); assertTrue(owned(c.decodeTask).buffer().capacity() <= 1024);
    c.decodeTask.close(); assertNull(owned(c.decodeTask));
  }
  @Test public void closeDoesNotBlockOrRecycleBufferDuringAnActiveHandler() throws Exception {
    List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received, true);
    java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.CountDownLatch finish = new java.util.concurrent.CountDownLatch(1);
    ServerAioHandler delegate = (ServerAioHandler)c.tioConfig.getAioHandler();
    ((ServerTioConfig)c.tioConfig).setServerAioHandler(new ServerAioHandler() {
      public Packet decode(ByteBuffer b,int l,int p,int r,ChannelContext x) throws Exception {return delegate.decode(b,l,p,r,x);}
      public ByteBuffer encode(Packet p,TioConfig config,ChannelContext x) { return null; }
      public void handler(Packet p,ChannelContext x) throws Exception {entered.countDown(); finish.await(5,java.util.concurrent.TimeUnit.SECONDS);}
    });
    c.decodeTask.decode(c,ByteBuffer.wrap(new byte[]{0,0}));
    Thread worker = new Thread(() -> c.decodeTask.decode(c,ByteBuffer.wrap(new byte[]{0,1,7})));
    worker.start();
    try {
      assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS));
      VirtualBuffer active = owned(c.decodeTask);
      Tio.close(c,"concurrent close");
      assertSame(owned(c.decodeTask),active);
    } finally { finish.countDown(); worker.join(5000); }
    assertFalse(worker.isAlive()); assertNull(owned(c.decodeTask));
  }
  @Test public void asynchronousPacketsSurviveInputReuseAndAccumulatorRelease() throws Exception {
    List<byte[]> received = Collections.synchronizedList(new ArrayList<byte[]>());
    ServerChannelContext c = context(received,true);
    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    java.util.concurrent.CountDownLatch unblock = new java.util.concurrent.CountDownLatch(1);
    executor.submit(() -> { try { unblock.await(); } catch (InterruptedException e) {Thread.currentThread().interrupt();} });
    c.tioConfig.setBizExecutor(executor);
    try {
      c.decodeTask.decode(c,ByteBuffer.wrap(new byte[]{0,0}));
      byte[] input = new byte[]{0,3,7,8,9};
      c.decodeTask.decode(c,ByteBuffer.wrap(input)); Arrays.fill(input,(byte)0);
      assertNull(owned(c.decodeTask)); assertTrue(received.isEmpty());
      c.decodeTask.decode(c,ByteBuffer.wrap(new byte[]{0,0,0,1,6}));
      unblock.countDown(); executor.shutdown();
      assertTrue(executor.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));
      assertEquals(2,received.size()); assertArrayEquals(new byte[]{7,8,9},received.get(0));
      assertArrayEquals(new byte[]{6},received.get(1));
    } finally {unblock.countDown(); executor.shutdownNow();}
  }
  @Test public void oneMiBFrameNeedsOnlyFiveAccumulatorCapacities() throws Exception {
    List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received,true);
    int chunk = 65536, frameSize = 1048576;
    byte[] wire = frame(new byte[frameSize - 4]);
    Set<ByteBuffer> buffers = Collections.newSetFromMap(new IdentityHashMap<ByteBuffer,Boolean>());
    long allocatedCapacity = 0;
    for (int offset=0; offset<wire.length; offset+=chunk) {
      c.decodeTask.decode(c,ByteBuffer.wrap(wire,offset,chunk));
      VirtualBuffer buffer = owned(c.decodeTask);
      if (buffer != null && buffers.add(buffer.buffer())) allocatedCapacity += buffer.buffer().capacity();
    }
    assertEquals(1,received.size()); assertNull(owned(c.decodeTask));
    assertEquals(5,buffers.size());
    assertEquals(2031616L,allocatedCapacity); // 64 + 128 + 256 + 512 + 1024 KiB
    long previousCompositeAndCopyCapacity = 0;
    for (int parts=1; parts<=16; parts++) {
      if (parts>1) previousCompositeAndCopyCapacity += (long)parts*chunk;
      if (parts<16) previousCompositeAndCopyCapacity += (long)parts*chunk;
    }
    assertEquals(16711680L,previousCompositeAndCopyCapacity);
    assertTrue(allocatedCapacity * 8 < previousCompositeAndCopyCapacity);
  }
  @Test public void sslPlaintextSharesTheSamePartialPacketAccumulator() throws Exception {
    List<byte[]> received = new ArrayList<>(); ServerChannelContext c = context(received, true);
    ReadCompletionHandler handler = new ReadCompletionHandler(c);
    handler.handlePlainFromSsl(ByteBuffer.wrap(new byte[]{0,0}));
    handler.handlePlainFromSsl(ByteBuffer.wrap(new byte[]{0,1,9}));
    assertEquals(1,received.size()); assertArrayEquals(new byte[]{9},received.get(0)); assertNull(owned(c.decodeTask));
  }
}
