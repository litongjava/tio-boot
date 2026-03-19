package com.litongjava.tio.proxy;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ProxyHandshake {

  private static final Charset HTTP_HDR_CHARSET = StandardCharsets.ISO_8859_1;
  private static final int MAX_HDR_BYTES = 64 * 1024;

  public static void httpConnect(AsynchronousSocketChannel ch, String targetHost, int targetPort, String user,
      String pass) throws Exception {

    StringBuilder sb = new StringBuilder();
    sb.append("CONNECT ").append(targetHost).append(":").append(targetPort).append(" HTTP/1.1\r\n");
    sb.append("Host: ").append(targetHost).append(":").append(targetPort).append("\r\n");
    sb.append("Proxy-Connection: Keep-Alive\r\n");

    if (user != null && pass != null) {
      String token = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
      sb.append("Proxy-Authorization: Basic ").append(token).append("\r\n");
    }
    sb.append("\r\n");

    writeFully(ch, ByteBuffer.wrap(sb.toString().getBytes(HTTP_HDR_CHARSET)));

    // 关键：必须读到 \r\n\r\n 才算 CONNECT 响应头完整，否则残留明文会污染 TLS 流
    ByteBuffer in = ByteBuffer.allocate(8192);
    StringBuilder respSb = new StringBuilder(256);

    while (true) {
      int n = ch.read(in).get();
      if (n <= 0) {
        throw new RuntimeException("proxy CONNECT no response");
      }

      in.flip();
      respSb.append(HTTP_HDR_CHARSET.decode(in));
      in.clear();

      if (respSb.length() > MAX_HDR_BYTES) {
        throw new RuntimeException("proxy CONNECT response header too large");
      }

      String resp = respSb.toString();
      int headerEnd = resp.indexOf("\r\n\r\n");
      if (headerEnd >= 0) {
        String header = resp.substring(0, headerEnd);

        // 状态行通常是第一行：HTTP/1.1 200 Connection established
        // 用 startsWith("HTTP/") + 包含 " 200 " 更稳
        if (!header.startsWith("HTTP/") || !header.contains(" 200 ")) {
          throw new RuntimeException("proxy CONNECT failed: " + header);
        }
        return;
      }
    }
  }

  public static void socks5Connect(AsynchronousSocketChannel ch, String targetHost, int targetPort, String user,
      String pass) throws Exception {

    writeFully(ch, ByteBuffer.wrap(new byte[] { 0x05, 0x01, 0x00 }));

    ByteBuffer resp = ByteBuffer.allocate(2);
    readFully(ch, resp);
    resp.flip();
    byte ver = resp.get();
    byte method = resp.get();
    if (ver != 0x05 || method != 0x00) {
      throw new RuntimeException("socks5 method not accepted: ver=" + ver + ", method=" + method);
    }

    byte[] host = targetHost.getBytes(StandardCharsets.UTF_8);
    ByteBuffer req = ByteBuffer.allocate(4 + 1 + host.length + 2);
    req.put((byte) 0x05);
    req.put((byte) 0x01);
    req.put((byte) 0x00);
    req.put((byte) 0x03);
    req.put((byte) host.length);
    req.put(host);
    req.putShort((short) targetPort);
    req.flip();
    writeFully(ch, req);

    ByteBuffer hdr = ByteBuffer.allocate(4);
    readFully(ch, hdr);
    hdr.flip();
    byte rver = hdr.get();
    byte rep = hdr.get();
    hdr.get();
    byte atyp = hdr.get();
    if (rver != 0x05 || rep != 0x00) {
      throw new RuntimeException("socks5 connect failed, rep=" + rep);
    }

    int addrLen;
    if (atyp == 0x01) {
      addrLen = 4;
    } else if (atyp == 0x04) {
      addrLen = 16;
    } else if (atyp == 0x03) {
      ByteBuffer l = ByteBuffer.allocate(1);
      readFully(ch, l);
      l.flip();
      addrLen = l.get() & 0xff;
    } else {
      throw new RuntimeException("unknown atyp=" + atyp);
    }

    ByteBuffer rest = ByteBuffer.allocate(addrLen + 2);
    readFully(ch, rest);
  }

  private static void writeFully(AsynchronousSocketChannel ch, ByteBuffer buf) throws Exception {
    while (buf.hasRemaining()) {
      ch.write(buf).get();
    }
  }

  private static void readFully(AsynchronousSocketChannel ch, ByteBuffer buf) throws Exception {
    while (buf.hasRemaining()) {
      int n = ch.read(buf).get();
      if (n < 0) {
        throw new RuntimeException("channel closed");
      }
    }
  }
}