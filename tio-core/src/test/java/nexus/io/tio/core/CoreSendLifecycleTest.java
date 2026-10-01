package nexus.io.tio.core;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import nexus.io.aio.Packet;
import nexus.io.aio.PacketMeta;
import nexus.io.tio.server.*;
import nexus.io.tio.server.intf.ServerAioHandler;
import nexus.io.tio.core.task.DecodeTask;

public class CoreSendLifecycleTest {
  @Test public void reopenedContextCanSendAfterCompletedClose() {
    ServerChannelContext x = context(new Encoder(), new Sink());
    Tio.close(x, "test reconnect");
    Sink replacement = new Sink(); x.setAsynchronousSocketChannel(replacement);
    x.setClosed(false); x.isRemoved = false; x.isWaitingClose = false;
    try {
      Packet p = packet(); assertTrue(Tio.send(x, p));
      assertEquals(Boolean.TRUE, p.getMeta().getIsSentSuccess()); assertEquals(1, replacement.writes);
    } finally { Tio.close(x, "test cleanup"); }
  }
  @Test public void zeroByteCompletionsRetryWithoutSpinning() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    Sink sink = new Sink() {
      public <A> void write(ByteBuffer b, long t, TimeUnit u, A x, CompletionHandler<Integer, ? super A> h) {
        if (attempts.incrementAndGet() <= 3) h.completed(0, x);
        else super.write(b, t, u, x, h);
      }
    };
    ServerChannelContext x = context(new Encoder(), sink);
    Packet p = packet();
    try {
      assertTrue(Tio.send(x, p));
      assertTrue(p.getMeta().getCountDownLatch().await(3, TimeUnit.SECONDS));
      assertEquals(Boolean.TRUE, p.getMeta().getIsSentSuccess());
      assertEquals(4, attempts.get());
    } finally { Tio.close(x, "test cleanup"); }
  }
  @Test public void splittingDirectBuffersKeepsExactChunksAndSourcePosition() {
    ByteBuffer source = ByteBuffer.allocateDirect(16387);
    source.position(3);
    for (int i = 0; i < 16384; i++) source.put((byte) i);
    source.flip(); source.position(3);
    ByteBuffer[] chunks = nexus.io.tio.core.utils.ByteBufferUtils.split(source, 8192);
    assertEquals(2, chunks.length); assertEquals(3, source.position());
    for (ByteBuffer chunk : chunks) {
      assertEquals(8192, chunk.remaining());
      for (int i = 0; i < 8192; i++) assertEquals((byte) i, chunk.get());
    }
  }
  static ServerChannelContext context(ServerAioHandler handler, Sink socket) {
    ServerTioConfig c=new ServerTioConfig("lifecycle-test");c.heartbeatTimeout=0;c.setServerAioHandler(handler);c.init();
    ServerChannelContext x=new ServerChannelContext(c,socket,"127.0.0.1",1234);x.setClosed(false);return x;
  }
  static class Encoder implements ServerAioHandler {
    boolean fail;
    public Packet decode(ByteBuffer b,int l,int p,int n,ChannelContext c) {return new NumberPacket(b.get());}
    public ByteBuffer encode(Packet p,TioConfig c,ChannelContext x) {
      if(fail)throw new IllegalStateException("encode failure");return ByteBuffer.wrap(new byte[]{'H'});
    }
    public void handler(Packet p,ChannelContext c) throws Exception {}
  }
  static class NumberPacket extends Packet {final int number;NumberPacket(int n){number=n;}}
  static Packet packet() {Packet p=new Packet();p.setKeepConnection(true);PacketMeta m=new PacketMeta();m.setCountDownLatch(new CountDownLatch(1));p.setMeta(m);return p;}
  static class Sink extends AsynchronousSocketChannel {
    CountDownLatch secondWritten;
    boolean open=true; int writes; List<String> sent=new CopyOnWriteArrayList<>();
    Sink() { super(AsynchronousChannelProvider.provider()); }
    public boolean isOpen(){return open;} public void close(){open=false;}
    public AsynchronousSocketChannel bind(SocketAddress a){return this;}
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> o,T v){return this;}
    public <T> T getOption(SocketOption<T> o){return null;}
    public Set<SocketOption<?>> supportedOptions(){return Collections.emptySet();}
    public AsynchronousSocketChannel shutdownInput(){return this;}
    public AsynchronousSocketChannel shutdownOutput(){return this;}
    public SocketAddress getRemoteAddress(){return new InetSocketAddress("127.0.0.1",1234);}
    public SocketAddress getLocalAddress(){return new InetSocketAddress("127.0.0.1",4321);}
    public <A> void connect(SocketAddress a,A x,CompletionHandler<Void,? super A> h){throw new UnsupportedOperationException();}
    public Future<Void> connect(SocketAddress a){throw new UnsupportedOperationException();}
    public <A> void read(ByteBuffer b,long t,TimeUnit u,A x,CompletionHandler<Integer,? super A> h){}
    public Future<Integer> read(ByteBuffer b){throw new UnsupportedOperationException();}
    public <A> void read(ByteBuffer[] b,int o,int n,long t,TimeUnit u,A x,CompletionHandler<Long,? super A> h){throw new UnsupportedOperationException();}
    public <A> void write(ByteBuffer b,long t,TimeUnit u,A x,CompletionHandler<Integer,? super A> h){
      int n=b.remaining(); byte[] data=new byte[n]; b.get(data); writes++; String wire=new String(data,StandardCharsets.UTF_8); sent.add(wire); h.completed(n,x);
      if(secondWritten!=null && wire.endsWith("/second")) secondWritten.countDown();
    }
    public Future<Integer> write(ByteBuffer b){throw new UnsupportedOperationException();}
    public <A> void write(ByteBuffer[] b,int o,int n,long t,TimeUnit u,A x,CompletionHandler<Long,? super A> h){throw new UnsupportedOperationException();}
  }

  static class HoldingSink extends Sink {
    CompletionHandler<Integer,Object> completion;Object attachment;ByteBuffer pending;
    @SuppressWarnings("unchecked") public <A> void write(ByteBuffer b,long t,TimeUnit u,A x,CompletionHandler<Integer,? super A> h) {
      if(completion!=null)throw new WritePendingException();pending=b;attachment=x;completion=(CompletionHandler<Integer,Object>)h;
    }
    void succeed() {CompletionHandler<Integer,Object> h=completion;Object a=attachment;int n=pending.remaining();byte[] data=new byte[n];pending.get(data);sent.add(new String(data,StandardCharsets.UTF_8));completion=null;h.completed(n,a);}
    public void close(){super.close();if(completion!=null){CompletionHandler<Integer,Object> h=completion;Object a=attachment;completion=null;h.failed(new ClosedChannelException(),a);}}
  }
  @Test public void encodingFailureClosesAndCompletesThePacket() {
    Encoder encoder=new Encoder();encoder.fail=true;Sink sink=new Sink();ServerChannelContext x=context(encoder,sink);Packet p=packet();
    assertFalse(Tio.send(x,p));assertEquals(0,p.getMeta().getCountDownLatch().getCount());assertEquals(Boolean.FALSE,p.getMeta().getIsSentSuccess());
    assertFalse(x.isSending.get());assertTrue(x.sendQueue.isEmpty());assertFalse(sink.open);assertFalse(Tio.send(x,packet()));
  }
  @Test public void synchronousSubmissionFailureCompletesAndReleasesState() {
    Sink sink=new Sink(){public <A> void write(ByteBuffer b,long t,TimeUnit u,A x,CompletionHandler<Integer,? super A> h){throw new IllegalStateException("submission failed");}};
    ServerChannelContext x=context(new Encoder(),sink);Packet p=packet();assertFalse(Tio.send(x,p));assertFalse(x.isSending.get());assertEquals(0,p.getMeta().getCountDownLatch().getCount());
  }
  @Test public void closingCompletesCurrentAndQueuedPackets() {
    HoldingSink sink=new HoldingSink();ServerChannelContext x=context(new Encoder(),sink);Packet a=packet(),b=packet();
    Tio.send(x,a);Tio.send(x,b);assertEquals(1,x.sendQueue.size());Tio.close(x,"test close");
    assertTrue(x.sendQueue.isEmpty());assertFalse(x.isSending.get());
    for(Packet p:Arrays.asList(a,b)){assertEquals(0,p.getMeta().getCountDownLatch().getCount());assertEquals(Boolean.FALSE,p.getMeta().getIsSentSuccess());}
  }
  @Test public void queuedFileSendsHeaderAndBodyThenNextPacket() throws Exception {
    Path file=Files.createTempFile("queued-body-",".txt");Files.write(file,"BODY".getBytes(StandardCharsets.US_ASCII));
    HoldingSink sink=new HoldingSink();ServerChannelContext x=context(new Encoder(),sink);
    try {
      Packet first=packet(),body=packet(),last=packet();body.setFileBody(file.toFile());Tio.send(x,first);Tio.send(x,body);Tio.send(x,last);
      for(int i=0;i<4;i++)sink.succeed();
      assertEquals(Arrays.asList("H","H","BODY","H"),sink.sent);assertEquals(4,body.getFileBodyTransferred());
      assertEquals(Boolean.TRUE,body.getMeta().getIsSentSuccess());assertFalse(x.isSending.get());assertTrue(x.sendQueue.isEmpty());
    } finally {Tio.close(x,"test cleanup");Files.deleteIfExists(file);}
  }
  @Test public void invalidFileRangeFailsBeforeSendingHeader() throws Exception {
    Path file=Files.createTempFile("invalid-range-",".txt");Sink sink=new Sink();ServerChannelContext x=context(new Encoder(),sink);
    try {Packet p=packet();p.setFileBody(file.toFile());p.setFileBodyLength(1);assertFalse(Tio.send(x,p));assertEquals(0,sink.writes);assertFalse(x.isSending.get());}
    finally {Tio.close(x,"cleanup");Files.deleteIfExists(file);}
  }
  @Test public void concurrentProducersDoNotLoseQueueWakeups() throws Exception {
    Sink sink=new Sink();ServerChannelContext x=context(new Encoder(),sink);ExecutorService pool=Executors.newFixedThreadPool(4);List<Packet> packets=new ArrayList<>();
    try {for(int i=0;i<400;i++){Packet p=packet();packets.add(p);pool.execute(()->Tio.send(x,p));}pool.shutdown();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));
      for(Packet p:packets){assertEquals(0,p.getMeta().getCountDownLatch().getCount());assertEquals(Boolean.TRUE,p.getMeta().getIsSentSuccess());}
      assertEquals(400,sink.writes);assertFalse(x.isSending.get());assertTrue(x.sendQueue.isEmpty());
    } finally {pool.shutdownNow();Tio.close(x,"cleanup");}
  }
  @Test public void decodedPacketsRunInConnectionOrder() throws Exception {
    CountDownLatch firstStarted=new CountDownLatch(1),release=new CountDownLatch(1),secondStarted=new CountDownLatch(1);List<Integer> order=new CopyOnWriteArrayList<>();
    Encoder handler=new Encoder(){public void handler(Packet p,ChannelContext c)throws Exception {int n=((NumberPacket)p).number;if(n==1){firstStarted.countDown();release.await(3,TimeUnit.SECONDS);}else secondStarted.countDown();order.add(n);}};
    Sink sink=new Sink();ServerChannelContext x=context(handler,sink);ExecutorService pool=Executors.newFixedThreadPool(2);x.tioConfig.setBizExecutor(pool);
    try {new DecodeTask().decode(x,ByteBuffer.wrap(new byte[]{1,2}));assertTrue(firstStarted.await(2,TimeUnit.SECONDS));assertFalse(secondStarted.await(100,TimeUnit.MILLISECONDS));release.countDown();pool.shutdown();assertTrue(pool.awaitTermination(3,TimeUnit.SECONDS));assertEquals(Arrays.asList(1,2),order);}
    finally {release.countDown();pool.shutdownNow();Tio.close(x,"cleanup");}
  }
  @Test public void malformedDecodeClosesInsteadOfSavingHalfPacket() {
    Encoder h=new Encoder(){public Packet decode(ByteBuffer b,int l,int p,int n,ChannelContext c){throw new IllegalArgumentException("invalid packet");}};
    Sink sink=new Sink();ServerChannelContext x=context(h,sink);new DecodeTask().decode(x,ByteBuffer.wrap(new byte[]{1}));assertFalse(sink.open);assertTrue(x.isClosed);
  }
}
