package nexus.io.tio.core;

import static org.junit.Assert.*;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.Test;
import nexus.io.aio.Packet;
import nexus.io.tio.core.pool.EncodedBuffer;
import nexus.io.tio.server.*;
import nexus.io.tio.server.intf.ServerAioHandler;

public class PooledTcpConnectionTest {
  static class Payload extends Packet {
    final byte[] bytes;
    Payload(byte[] bytes) { this.bytes = bytes; setKeepConnection(true); }
  }
  @Test public void customTcpCodecUsesPooledOutputAcrossMultipleRequests() throws Exception {
    ServerAioHandler codec = new ServerAioHandler() {
      public Packet decode(ByteBuffer b,int limit,int position,int readable,ChannelContext x) {
        if (readable < 4) return null;
        int size = b.getInt(); if (size < 0 || size > 65536) throw new IllegalArgumentException("Invalid test frame size");
        if (b.remaining() < size) return null;
        byte[] bytes = new byte[size]; b.get(bytes); return new Payload(bytes);
      }
      public ByteBuffer encode(Packet p,TioConfig c,ChannelContext x) { throw new AssertionError("Use the owned encoding path"); }
      public EncodedBuffer encodeBuffer(Packet p,TioConfig c,ChannelContext x) {
        byte[] bytes = ((Payload) p).bytes;
        return EncodedBuffer.encodePooled(allocate -> {
          ByteBuffer output = allocate.apply(4 + bytes.length); output.putInt(bytes.length).put(bytes).flip(); return output;
        });
      }
      public void handler(Packet p,ChannelContext x) { Tio.send(x,p); }
    };
    ServerTioConfig config = new ServerTioConfig("pooled-tcp-test"); config.setServerAioHandler(codec);
    config.setWorkerThreads(1); config.heartbeatTimeout = 0;
    TioServer server = new TioServer(config); server.start("127.0.0.1",0);
    try (Socket socket = new Socket()) {
      socket.connect(server.getServerSocketChannel().getLocalAddress()); socket.setSoTimeout(5000);
      DataOutputStream output = new DataOutputStream(socket.getOutputStream()); DataInputStream input = new DataInputStream(socket.getInputStream());
      for (int size : new int[]{3, 9000, 65536, 1}) {
        byte[] body = new byte[size]; new Random(size).nextBytes(body);
        output.writeInt(size); output.write(body); output.flush();
        assertEquals(size,input.readInt()); byte[] received = new byte[size]; input.readFully(received); assertArrayEquals(body,received);
      }
    } finally { server.stop(); }
  }
}
