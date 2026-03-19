package com.litongjava.tio.http.server.util;

import java.nio.charset.StandardCharsets;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.encoder.ChunkEncoder;
import com.litongjava.tio.http.common.sse.ChunkedPacket;

public class SseEmitter {

  public static String LFLF = "\n\n";

  public static boolean pushSSEChunk(ChannelContext channelContext, String string) {
    String text = "data:" + string + LFLF;
    byte[] bytes = text.getBytes();
    ChunkedPacket ssePacket = new ChunkedPacket(ChunkEncoder.encodeChunk(bytes));
    return Tio.bSend(channelContext, ssePacket);
  }

  public static boolean pushSSEChunk(ChannelContext channelContext, String event, String data) {
    StringBuilder sb = new StringBuilder();
    if (event != null) {
      sb.append("event:").append(event).append("\n");
    }
    sb.append("data:").append(data).append(LFLF);
    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
    ChunkedPacket ssePacket = new ChunkedPacket(ChunkEncoder.encodeChunk(bytes));
    return Tio.bSend(channelContext, ssePacket);
  }

  public static boolean pushChunk(ChannelContext channelContext, byte[] bytes) {
    ChunkedPacket ssePacket = new ChunkedPacket(ChunkEncoder.encodeChunk(bytes));
    return Tio.bSend(channelContext, ssePacket);
  }

  public static void closeSeeConnection(ChannelContext channelContext) {
    Tio.remove(channelContext, "remove");
  }

  public static void closeChunkConnectionImmediately(ChannelContext channelContext) {
    // 关闭连接
    byte[] zeroChunk = ChunkEncoder.encodeChunk(new byte[0]);
    ChunkedPacket endPacket = new ChunkedPacket(zeroChunk);
    Tio.bSend(channelContext, endPacket);
    Tio.close(channelContext, "remove");
  }
  public static void closeChunkConnection(ChannelContext channelContext) {
    // 关闭连接
    byte[] zeroChunk = ChunkEncoder.encodeChunk(new byte[0]);
    ChunkedPacket endPacket = new ChunkedPacket(zeroChunk);
    Tio.bSend(channelContext, endPacket);
    Tio.remove(channelContext, "remove");
  }

}
