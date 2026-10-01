package nexus.io.tio.websocket.common;

import java.nio.ByteBuffer;
import nexus.io.tio.core.pool.EncodedBuffer;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.TioConfig;

public class WebSocketClientEncoder {


  /** 
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-------+-+-------------+-------------------------------+
     |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
     |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
     |N|V|V|V|       |S|             |   (if payload len==126/127)   |
     | |1|2|3|       |K|             |                               |
     +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
     |     Extended payload length continued, if payload len == 127  |
     + - - - - - - - - - - - - - - - +-------------------------------+
     |                               |Masking-key, if MASK set to 1  |
     +-------------------------------+-------------------------------+
     | Masking-key (continued)       |          Payload Data         |
     +-------------------------------- - - - - - - - - - - - - - - - +
     :                     Payload Data continued ...                :
     + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
     |                     Payload Data continued ...                |
     +---------------------------------------------------------------+
  */
  public static ByteBuffer encode(WebSocketPacket packet, TioConfig tioConfig, ChannelContext channelContext) {
    return WebSocketEncoder.encode(packet, true, ByteBuffer::allocate);
  }

  public static EncodedBuffer encodeBuffer(WebSocketPacket packet, TioConfig tioConfig, ChannelContext channelContext) {
    return EncodedBuffer.encodePooled(allocator -> {
      ByteBuffer buffer = WebSocketEncoder.encode(packet, true, allocator);
      buffer.flip();
      return buffer;
    });
  }
}
