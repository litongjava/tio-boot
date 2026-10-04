package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;

public class StringByteConversionTest {
  private static final String TEXT = "Hello \u4e2d\ud83d\ude00";

  @Test
  public void primitiveBytesUseRequestedCharset() {
    assertEquals(TEXT, StrUtil.utf8Str(TEXT.getBytes(StandardCharsets.UTF_8)));
    assertEquals(TEXT, StrUtil.str(TEXT.getBytes(StandardCharsets.UTF_16LE), "UTF-16LE"));
    assertEquals("", StrUtil.utf8Str(new byte[0]));
  }

  @Test
  public void boxedBytesAreDecodedAsText() {
    byte[] bytes = TEXT.getBytes(StandardCharsets.UTF_8);
    Byte[] boxed = new Byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      boxed[i] = bytes[i];
    }
    assertEquals(TEXT, StrUtil.utf8Str(boxed));
    assertEquals("", StrUtil.utf8Str(new Byte[0]));
  }

  private void assertBuffer(ByteBuffer buffer) {
    int position = buffer.position();
    int limit = buffer.limit();
    buffer.mark();
    assertEquals(TEXT, StrUtil.utf8Str(buffer));
    assertEquals(position, buffer.position());
    assertEquals(limit, buffer.limit());
    buffer.reset();
    assertEquals(position, buffer.position());
  }

  @Test
  public void bufferViewsDecodeOnlyRemainingBytesAndPreserveState() {
    byte[] bytes = TEXT.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
    buffer.put((byte) 99).put((byte) 99).put(bytes).put((byte) 99);
    buffer.position(2);
    buffer.limit(2 + bytes.length);
    assertBuffer(buffer);
    assertBuffer(buffer.slice());
    assertBuffer(buffer.asReadOnlyBuffer());
    ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length);
    direct.put(bytes).flip();
    assertBuffer(direct);
    assertEquals("", StrUtil.utf8Str(ByteBuffer.allocate(0)));
  }

  @Test
  public void malformedBytesUseConsistentReplacement() {
    byte[] malformed = {(byte) 0xc3, 0x28};
    String expected = new String(malformed, StandardCharsets.UTF_8);
    assertEquals(expected, StrUtil.utf8Str(malformed));
    assertEquals(expected, StrUtil.utf8Str(ByteBuffer.wrap(malformed)));
  }

  @Test
  public void ordinaryObjectsAndArraysRetainTheirConversion() {
    assertNull(StrUtil.utf8Str(null));
    assertEquals("text", StrUtil.utf8Str("text"));
    assertEquals("[1, 2]", StrUtil.utf8Str(new int[] {1, 2}));
    assertEquals("[a, b]", StrUtil.utf8Str(new String[] {"a", "b"}));
    assertEquals("42", StrUtil.utf8Str(Integer.valueOf(42)));
  }
}
