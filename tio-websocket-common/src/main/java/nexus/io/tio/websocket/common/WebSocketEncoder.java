package nexus.io.tio.websocket.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.function.IntFunction;

/** Writes every frame field explicitly so pooled storage needs no zero filling. */
final class WebSocketEncoder {
  private static final SecureRandom MASK_RANDOM = new SecureRandom();
  private WebSocketEncoder() {}

  static ByteBuffer encode(WebSocketPacket packet, boolean masked, IntFunction<ByteBuffer> allocator) {
    byte[] body = packet.getBody();
    byte[][] bodies = packet.getBodys();
    long length = 0;
    if (body != null) length = body.length;
    else if (bodies != null) for (byte[] part : bodies) length += part.length;
    int headerLength = length < 126 ? 2 : length <= 65535 ? 4 : 10;
    long frameLength = length + headerLength + (masked ? 4 : 0);
    if (frameLength > Integer.MAX_VALUE) throw new IllegalArgumentException("WebSocket frame is too large");
    byte opcode = packet.getWsOpcode().getCode();
    ByteBuffer output = allocator.apply((int) frameLength).order(ByteOrder.BIG_ENDIAN);
    int fin = !masked || packet.isWsEof() ? 0x80 : 0;
    output.put((byte) (fin | (opcode & 0x0f)));
    int maskBit = masked ? 0x80 : 0;
    if (length < 126) output.put((byte) (maskBit | (int) length));
    else if (length <= 65535) { output.put((byte) (maskBit | 126)); output.putShort((short) length); }
    else { output.put((byte) (maskBit | 127)); output.putLong(length); }
    int mask = masked ? MASK_RANDOM.nextInt() : 0;
    if (masked) output.putInt(mask);
    int offset = 0;
    if (body != null) writeBody(output, body, masked, mask, offset);
    else if (bodies != null) for (byte[] part : bodies) {
      writeBody(output, part, masked, mask, offset);
      offset += part.length;
    }
    return output;
  }

  private static void writeBody(ByteBuffer output, byte[] body, boolean masked, int mask, int offset) {
    if (!masked) { output.put(body); return; }
    for (int i = 0; i < body.length; i++) {
      int shift = (3 - ((offset + i) & 3)) * 8;
      output.put((byte) (body[i] ^ (mask >>> shift)));
    }
  }
}
