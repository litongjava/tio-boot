package nexus.io.tio.websocket.common;

import java.nio.ByteBuffer;
import nexus.io.tio.core.pool.EncodedBuffer;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.TioConfig;

/**
 * 参考了baseio: https://gitee.com/generallycloud/baseio
 * com.generallycloud.nio.codec.http11.WebSocketProtocolEncoder
 * @author tanyaowu
 *
 */
public class WebSocketServerEncoder {
  public static final int MAX_HEADER_LENGTH = 20480;

  private static void checkLength(byte[] bytes, int length, int offset) {
    if (bytes == null) {
      throw new IllegalArgumentException("null");
    }

    if (offset < 0) {
      throw new IllegalArgumentException("invalidate offset " + offset);
    }

    if (bytes.length - offset < length) {
      throw new IllegalArgumentException("invalidate length " + bytes.length);
    }
  }

  public static ByteBuffer encode(WebSocketResponse wsResponse, TioConfig tioConfig, ChannelContext channelContext) {
    return WebSocketEncoder.encode(wsResponse, false, ByteBuffer::allocate);
  }

  public static EncodedBuffer encodeBuffer(WebSocketResponse wsResponse, TioConfig tioConfig, ChannelContext channelContext) {
    return EncodedBuffer.encodePooled(allocator -> {
      ByteBuffer buffer = WebSocketEncoder.encode(wsResponse, false, allocator);
      buffer.flip();
      return buffer;
    });
  }

  public static void int2Byte(byte[] bytes, int value, int offset) {
    checkLength(bytes, 4, offset);

    bytes[offset + 3] = (byte) (value & 0xff);
    bytes[offset + 2] = (byte) (value >> 8 * 1 & 0xff);
    bytes[offset + 1] = (byte) (value >> 8 * 2 & 0xff);
    bytes[offset + 0] = (byte) (value >> 8 * 3);
  }

  /**
   *
   * @author tanyaowu
   * 2017年2月22日 下午4:06:42
   *
   */
  public WebSocketServerEncoder() {

  }
}
