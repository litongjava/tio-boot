package nexus.io.tio.boot.context;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import nexus.io.tio.boot.server.TioBootServerHandler;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.exception.UnsupportedHttpMethodException;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.common.handler.ITioHttpRequestHandler;
import nexus.io.tio.http.server.HttpServerAioHandler;
import nexus.io.tio.server.ServerChannelContext;
import nexus.io.tio.server.ServerTioConfig;

public class HttpErrorCloseTest {
  @Test
  public void unsupportedMethodFlushesErrorBeforeClosingAndStopsFurtherDecode() throws Exception {
    TioBootServerHandler handler = new TioBootServerHandler(null, null, new HttpConfig(0, false), null, null, null);
    ServerTioConfig config = new ServerTioConfig("error-close-test");
    config.heartbeatTimeout = 0;
    config.setServerAioHandler(handler);
    config.init();
    HoldingSocket socket = new HoldingSocket();
    ServerChannelContext context = new ServerChannelContext(config, socket, "127.0.0.1", 1234);
    context.setClosed(false);
    try {
      Method reject = TioBootServerHandler.class.getDeclaredMethod("handleUnsupportedHttpMethod",
          ChannelContext.class, UnsupportedHttpMethodException.class);
      reject.setAccessible(true);
      reject.invoke(handler, context, new UnsupportedHttpMethodException("INVALID"));
      assertTrue("Error response is still pending", socket.isOpen());
      assertNotNull(socket.pending);
      ByteBuffer next = ByteBuffer.wrap("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
      assertNull(handler.decode(next, next.limit(), 0, next.remaining(), context));
      assertEquals(next.limit(), next.position());
      socket.complete();
      String wire = new String(socket.bytes.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(wire.startsWith("HTTP/1.1 405"));
      assertTrue(wire.toLowerCase().contains("connection:close"));
      assertTrue(wire.endsWith("Unsupported HTTP method: INVALID"));
      assertFalse("Close only after complete response", socket.isOpen());
    } finally {
      Tio.close(context, "test cleanup");
    }
  }

  @Test
  public void noLaterBusinessRequestRunsWhileClosingResponseIsStillPending() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    ITioHttpRequestHandler business = (ITioHttpRequestHandler) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] {ITioHttpRequestHandler.class}, (proxy, method, args) -> {
          if ("handler".equals(method.getName())) {
            calls.incrementAndGet();
            return new HttpResponse((HttpRequest) args[0]).body("last").addHeader("Connection", "close");
          }
          return null;
        });
    HttpConfig http = new HttpConfig(0, false);
    HttpServerAioHandler handler = new HttpServerAioHandler(http, business);
    ServerTioConfig config = new ServerTioConfig("pipeline-close-test");
    config.heartbeatTimeout = 0;
    config.setServerAioHandler(handler);
    config.init();
    HoldingSocket socket = new HoldingSocket();
    ServerChannelContext context = new ServerChannelContext(config, socket, "127.0.0.1", 1234);
    context.setClosed(false);
    HttpRequest request = new HttpRequest() {
      @Override public String getClientIp() { return "127.0.0.1"; }
    };
    request.requestLine = new RequestLine();
    request.requestLine.setMethod(HttpMethod.GET);
    request.requestLine.setVersion("1.1");
    request.requestLine.setPath("/last");
    request.httpConfig = http;
    try {
      handler.handler(request, context);
      assertTrue(socket.isOpen());
      handler.handler(request, context);
      assertEquals(1, calls.get());
      socket.complete();
      assertFalse(socket.isOpen());
    } finally {
      Tio.close(context, "test cleanup");
    }
  }

  private static class HoldingSocket extends AsynchronousSocketChannel {
    private boolean open = true;
    private ByteBuffer pending;
    private Object attachment;
    private CompletionHandler<Integer, Object> callback;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    HoldingSocket() { super(AsynchronousChannelProvider.provider()); }
    public boolean isOpen() { return open; }
    public void close() { open = false; }
    public AsynchronousSocketChannel bind(SocketAddress address) { return this; }
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> option, T value) { return this; }
    public <T> T getOption(SocketOption<T> option) { return null; }
    public Set<SocketOption<?>> supportedOptions() { return Collections.emptySet(); }
    public AsynchronousSocketChannel shutdownInput() { return this; }
    public AsynchronousSocketChannel shutdownOutput() { return this; }
    public SocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 1234); }
    public SocketAddress getLocalAddress() { return new InetSocketAddress("127.0.0.1", 4321); }
    public <A> void connect(SocketAddress address, A value, CompletionHandler<Void, ? super A> handler) { throw new UnsupportedOperationException(); }
    public Future<Void> connect(SocketAddress address) { throw new UnsupportedOperationException(); }
    public <A> void read(ByteBuffer buffer, long timeout, TimeUnit unit, A value, CompletionHandler<Integer, ? super A> handler) { }
    public Future<Integer> read(ByteBuffer buffer) { throw new UnsupportedOperationException(); }
    public <A> void read(ByteBuffer[] buffers, int offset, int length, long timeout, TimeUnit unit, A value, CompletionHandler<Long, ? super A> handler) { throw new UnsupportedOperationException(); }
    @SuppressWarnings("unchecked")
    public <A> void write(ByteBuffer buffer, long timeout, TimeUnit unit, A value, CompletionHandler<Integer, ? super A> handler) {
      pending = buffer;
      attachment = value;
      callback = (CompletionHandler<Integer, Object>) handler;
    }
    public Future<Integer> write(ByteBuffer buffer) { throw new UnsupportedOperationException(); }
    public <A> void write(ByteBuffer[] buffers, int offset, int length, long timeout, TimeUnit unit, A value, CompletionHandler<Long, ? super A> handler) { throw new UnsupportedOperationException(); }
    void complete() {
      int count = pending.remaining();
      byte[] content = new byte[count];
      pending.get(content);
      bytes.write(content, 0, content.length);
      callback.completed(count, attachment);
    }
  }
}
