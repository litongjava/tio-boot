package nexus.io.tio.websocket.server;

import static org.junit.Assert.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Test;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.pool.EncodedBuffer;
import nexus.io.tio.http.common.*;
import nexus.io.tio.server.*;
import nexus.io.tio.websocket.common.*;
import nexus.io.tio.websocket.server.handler.IWebSocketHandler;

public class PooledWebSocketConnectionTest {
  @Test public void handshakeAndMultipleFramesReuseOneConnection() throws Exception {
    WebsocketServerConfig ws = new WebsocketServerConfig(0, false);
    IWebSocketHandler echo = new IWebSocketHandler() {
      public HttpResponse handshake(HttpRequest r,HttpResponse p,ChannelContext x) { return p; }
      public void onAfterHandshaked(HttpRequest r,HttpResponse p,ChannelContext x) {}
      public Object onBytes(WebSocketRequest r,byte[] b,ChannelContext x) { return b; }
      public Object onText(WebSocketRequest r,String s,ChannelContext x) { return s; }
      public Object onClose(WebSocketRequest r,byte[] b,ChannelContext x) { return null; }
    };
    ServerTioConfig config = new ServerTioConfig("pooled-websocket-test");
    config.setWorkerThreads(1); config.heartbeatTimeout = 0;
    config.setServerAioHandler(new WebsocketServerAioHandler(ws, echo));
    config.setServerAioListener(new WebSocketServerAioListener());
    TioServer server = new TioServer(config); server.start("127.0.0.1", 0);
    try (Socket socket = new Socket()) {
      socket.connect(server.getServerSocketChannel().getLocalAddress()); socket.setSoTimeout(5000);
      OutputStream output = socket.getOutputStream(); DataInputStream input = new DataInputStream(socket.getInputStream());
      output.write(("GET / HTTP/1.1\r\nHost: localhost\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
          + "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
      output.flush(); assertTrue(line(input).contains(" 101 "));
      while (!line(input).isEmpty()) {}
      for (int size : new int[]{5, 126, 65536, 7}) {
        byte[] body = new byte[size]; new Random(size).nextBytes(body);
        WebSocketPacket packet = new WebSocketPacket(body); packet.setWsEof(true);
        try (EncodedBuffer encoded = WebSocketClientEncoder.encodeBuffer(packet, null, null)) {
          ByteBuffer bytes = encoded.buffer(); byte[] wire = new byte[bytes.remaining()]; bytes.get(wire); output.write(wire); output.flush();
        }
        assertEquals(0x82, input.readUnsignedByte()); int length = input.readUnsignedByte(); assertEquals(0, length & 128);
        long actual = length == 126 ? input.readUnsignedShort() : length == 127 ? input.readLong() : length;
        assertEquals(size, actual); byte[] received = new byte[size]; input.readFully(received); assertArrayEquals(body, received);
      }
    } finally { server.stop(); }
  }
  private static String line(InputStream input) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    for (int n; (n = input.read()) != '\n';) {
      if (n < 0) throw new EOFException(); if (n != '\r') bytes.write(n);
      if (bytes.size() > 8192) throw new IOException("Unexpected response header size");
    }
    return bytes.toString("US-ASCII");
  }
}
