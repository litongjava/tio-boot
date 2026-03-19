package com.litongjava.tio.http.client.handler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.litongjava.aio.Packet;
import com.litongjava.tio.client.intf.ClientAioHandler;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.http.client.packet.ChunkParseResult;
import com.litongjava.tio.http.client.packet.HttpRequestPacket;
import com.litongjava.tio.http.client.packet.HttpResponsePacket;

public class SimpleHttpClientAioHandler implements ClientAioHandler {

  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    if (!(packet instanceof HttpRequestPacket)) {
      throw new IllegalArgumentException("unsupported packet: " + packet);
    }
    byte[] bytes = ((HttpRequestPacket) packet).getBytes();
    return ByteBuffer.wrap(bytes);
  }

  @Override
  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext)
      throws Exception {

    // 只信 buffer 自己的 position/limit，避免和上层传参不一致
    int start = buffer.position();
    int end = buffer.limit();

    // 1) 找到 header 结束
    int headerEnd = indexOf(buffer, start, end, CRLFCRLF);
    if (headerEnd < 0) {
      return null;
    }

    int headersBytesEnd = headerEnd + 4;

    // 2) 解析 headers 文本
    String headerText = readIso88591(buffer, start, headersBytesEnd);
    String[] lines = headerText.split("\r\n");
    if (lines.length == 0)
      return null;

    HttpResponsePacket resp = new HttpResponsePacket();
    resp.statusLine = lines[0];

    String[] parts = lines[0].split(" ");
    if (parts.length >= 2) {
      try {
        resp.statusCode = Integer.parseInt(parts[1]);
      } catch (Exception ignore) {
      }
    }

    boolean chunked = false;
    int contentLength = -1;

    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];
      int idx = line.indexOf(':');
      if (idx <= 0)
        continue;
      String k = line.substring(0, idx).trim();
      String v = line.substring(idx + 1).trim();
      resp.headers.put(k, v);

      String kl = k.toLowerCase(Locale.ROOT);
      if ("content-length".equals(kl)) {
        try {
          contentLength = Integer.parseInt(v.trim());
        } catch (Exception ignore) {
        }
      } else if ("transfer-encoding".equals(kl) && v.toLowerCase(Locale.ROOT).contains("chunked")) {
        chunked = true;
      }
    }

    int bodyStart = headersBytesEnd;

    // 3) chunked
    if (chunked) {
      ChunkParseResult cpr = parseChunkedBody(buffer, bodyStart, end);
      if (cpr == null)
        return null;

      resp.body = cpr.body;

      // 消费：从 start 到 bodyStart+cpr.consumedBytes
      int consumedEnd = bodyStart + cpr.consumedBytes;
      buffer.position(consumedEnd);
      return resp;
    }

    // 4) content-length
    if (contentLength >= 0) {
      int needEnd = bodyStart + contentLength;
      if (needEnd > end)
        return null;

      byte[] body = new byte[contentLength];
      for (int i = 0; i < contentLength; i++) {
        body[i] = buffer.get(bodyStart + i);
      }
      resp.body = body;
      buffer.position(needEnd);
      return resp;
    }

    // 5) 既不是 chunked 也没 content-length：按 RFC 需要靠连接关闭判定结束
    // 这里不要“直接把 end-bodyStart 当 body 并吃掉”，因为连接可能还没关闭，数据还没收全
    // 先返回 null，等对端 close（Read=-1）时你再把缓存 flush 成响应（如果你要支持这种情况）
    return null;
  }

  private static String readIso88591(ByteBuffer buf, int s, int e) {
    byte[] b = new byte[e - s];
    for (int i = 0; i < b.length; i++)
      b[i] = buf.get(s + i);
    return new String(b, StandardCharsets.ISO_8859_1);
  }

  @Override
  public Packet heartbeatPacket(ChannelContext channelContext) {
    return null;
  }

  private static final byte[] CRLFCRLF = new byte[] { '\r', '\n', '\r', '\n' };

  private static int indexOf(ByteBuffer buf, int start, int end, byte[] pat) {
    outer: for (int i = start; i <= end - pat.length; i++) {
      for (int j = 0; j < pat.length; j++) {
        if (buf.get(i + j) != pat[j])
          continue outer;
      }
      return i;
    }
    return -1;
  }

  private static ChunkParseResult parseChunkedBody(ByteBuffer buf, int bodyStart, int end) {
    int p = bodyStart;
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

    while (true) {
      int lineEnd = indexOf(buf, p, end, new byte[] { '\r', '\n' });
      if (lineEnd < 0)
        return null;
      String hex = readAscii(buf, p, lineEnd).trim();
      int semi = hex.indexOf(';');
      if (semi >= 0)
        hex = hex.substring(0, semi).trim();

      int size;
      try {
        size = Integer.parseInt(hex, 16);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      p = lineEnd + 2;

      if (size == 0) {
        // 终止块后还有一行空行 \r\n（以及可能的 trailer，这里忽略 trailer，找 \r\n 结束）
        int trailerEnd = indexOf(buf, p, end, CRLFCRLF);
        if (trailerEnd < 0) {
          // 有些实现只有 \r\n
          int simple = indexOf(buf, p, end, new byte[] { '\r', '\n' });
          if (simple < 0)
            return null;
          p = simple + 2;
        } else {
          p = trailerEnd + 4;
        }
        ChunkParseResult r = new ChunkParseResult();
        r.body = baos.toByteArray();
        r.consumedBytes = p - bodyStart;
        return r;
      }

      if (p + size + 2 > end)
        return null;
      for (int i = 0; i < size; i++)
        baos.write(buf.get(p + i));
      p += size;

      // 读 chunk 末尾 CRLF
      if (p + 2 > end)
        return null;
      if (buf.get(p) != '\r' || buf.get(p + 1) != '\n')
        throw new RuntimeException("invalid chunk ending");
      p += 2;
    }
  }

  private static String readAscii(ByteBuffer buf, int s, int e) {
    byte[] b = new byte[e - s];
    for (int i = 0; i < b.length; i++)
      b[i] = buf.get(s + i);
    return new String(b, StandardCharsets.US_ASCII);
  }

  @Override
  public void handler(Packet packet, ChannelContext channelContext) throws Exception {

  }

}
