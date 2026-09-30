package nexus.io.tio.core;

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import nexus.io.enhance.buffer.VirtualBuffer;
import nexus.io.tio.server.*;

public class IoCallbackCleanupTest {
  private static ServerTioConfig config(String name) {
    ServerTioConfig config = new ServerTioConfig(name);
    config.heartbeatTimeout = 0;
    config.init();
    return config;
  }

  private static void assertReleased(VirtualBuffer buffer) {
    assertNotNull(buffer);
    try { buffer.clean(); fail("buffer was not released"); }
    catch (UnsupportedOperationException expected) { }
  }

  private ServerChannelContext context(ServerTioConfig config, FakeSocket socket) {
    ServerChannelContext context = new ServerChannelContext(config, socket, "127.0.0.1", 1234);
    context.setClosed(false);
    context.packetNeededLength = 100;
    return context;
  }

  @Test public void allReadFailuresReleaseBufferAndCloseConnection() {
    ServerTioConfig config = config("read-failure");
    FakeSocket socket = new FakeSocket();
    ServerChannelContext context = context(config, socket);
    VirtualBuffer buffer = VirtualBuffer.wrap(ByteBuffer.allocate(8));
    new ReadCompletionHandler(context).failed(new IOException("read failed"), buffer);
    assertReleased(buffer);
    assertFalse(socket.isOpen());
    assertEquals(0, config.connections.size());
  }

  @Test public void eofAndInvalidReadResultsReleaseBuffer() {
    for (int result : new int[] {0, -1, -4}) {
      FakeSocket socket = new FakeSocket();
      ServerChannelContext context = context(config("read-result"), socket);
      VirtualBuffer buffer = VirtualBuffer.wrap(ByteBuffer.allocate(8));
      new ReadCompletionHandler(context).completed(result, buffer);
      assertReleased(buffer);
      assertFalse(socket.isOpen());
    }
  }

  @Test public void resizedBufferIsReleasedWhenReadSubmissionThrows() {
    FakeSocket socket = new FakeSocket();
    ServerChannelContext context = context(config("resize"), socket);
    context.setReadBufferSize(16);
    socket.throwOnRead = true;
    VirtualBuffer old = VirtualBuffer.wrap(ByteBuffer.allocate(8));
    old.buffer().put((byte) 1);
    new ReadCompletionHandler(context).completed(1, old);
    assertReleased(old);
    assertNotSame(old, socket.readBuffer);
    assertReleased(socket.readBuffer);
    assertFalse(socket.isOpen());
  }

  @Test public void successfulReadTransfersBufferOwnership() {
    FakeSocket socket = new FakeSocket();
    ServerChannelContext context = context(config("read-owner"), socket);
    context.setReadBufferSize(8);
    VirtualBuffer buffer = VirtualBuffer.wrap(ByteBuffer.allocate(8));
    buffer.buffer().put((byte) 1);
    ReadCompletionHandler handler = new ReadCompletionHandler(context);
    handler.completed(1, buffer);
    assertSame(buffer, socket.readBuffer);
    assertTrue(socket.isOpen());
    handler.failed(new IOException("later failure"), buffer);
    assertReleased(buffer);
    assertFalse(socket.isOpen());
  }

  @Test public void shutdownAndMissingListenerCloseAcceptedSocket() {
    for (boolean stopping : new boolean[] {true, false}) {
      TioServer server = new TioServer(config("stopping"));
      server.setWaitingStop(stopping);
      FakeSocket socket = new FakeSocket();
      new AcceptCompletionHandler().completed(socket, server);
      assertFalse(socket.isOpen());
    }
  }

  @Test public void initializationFailureReleasesBufferAndRegisteredContext() throws Exception {
    ServerTioConfig config = config("initial-read-failure");
    try (AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open()) {
      // The unbound listener fails to rearm, but initialization of the accepted socket must still run.
      TioServer server = new TioServer(config) {
        @Override public AsynchronousServerSocketChannel getServerSocketChannel() { return listener; }
      };
      FakeSocket socket = new FakeSocket();
      socket.throwOnRead = true;
      new AcceptCompletionHandler().completed(socket, server);
      assertReleased(socket.readBuffer);
      assertFalse(socket.isOpen());
      assertEquals(0, config.connections.size());
    }
  }

  @Test public void earlyInitializationFailureClosesSocket() throws Exception {
    try (AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open()) {
      TioServer server = new TioServer(config("option-failure")) {
        @Override public AsynchronousServerSocketChannel getServerSocketChannel() { return listener; }
      };
      FakeSocket socket = new FakeSocket();
      socket.throwOnOption = true;
      new AcceptCompletionHandler().completed(socket, server);
      assertFalse(socket.isOpen());
      assertNull(socket.readBuffer);
    }
  }

  private static class FakeSocket extends AsynchronousSocketChannel {
    boolean open = true, throwOnRead, throwOnOption;
    VirtualBuffer readBuffer;
    FakeSocket() { super(AsynchronousChannelProvider.provider()); }
    public boolean isOpen() { return open; }
    public void close() { open = false; }
    public AsynchronousSocketChannel bind(SocketAddress local) { return this; }
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
      if (throwOnOption) throw new IOException("option failure"); return this;
    }
    public <T> T getOption(SocketOption<T> name) { return null; }
    public Set<SocketOption<?>> supportedOptions() { return Collections.emptySet(); }
    public AsynchronousSocketChannel shutdownInput() { return this; }
    public AsynchronousSocketChannel shutdownOutput() { return this; }
    public SocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1",1234); }
    public SocketAddress getLocalAddress() { return new InetSocketAddress("127.0.0.1",4321); }
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void,? super A> handler) { throw new UnsupportedOperationException(); }
    public Future<Void> connect(SocketAddress remote) { throw new UnsupportedOperationException(); }
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer,? super A> handler) {
      readBuffer = (VirtualBuffer) attachment;
      if (throwOnRead) throw new IllegalStateException("read submission failed");
    }
    public Future<Integer> read(ByteBuffer dst) { throw new UnsupportedOperationException(); }
    public <A> void read(ByteBuffer[] dst, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long,? super A> handler) { throw new UnsupportedOperationException(); }
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer,? super A> handler) { throw new UnsupportedOperationException(); }
    public Future<Integer> write(ByteBuffer src) { throw new UnsupportedOperationException(); }
    public <A> void write(ByteBuffer[] src, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long,? super A> handler) { throw new UnsupportedOperationException(); }
  }
}
