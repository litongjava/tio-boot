package nexus.io.tio.core;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import org.junit.Test;
import nexus.io.tio.core.utils.ByteBufferUtils;

public class ByteBufferCopyBoundaryTest {
  @Test
  public void slicedCopyUsesBufferIndexesInsteadOfBackingArrayIndexes() {
    ByteBuffer source = ByteBuffer.wrap(new byte[] {9, 8, 1, 2, 3});
    source.position(2);
    source = source.slice();
    ByteBuffer destination = ByteBuffer.allocate(6);
    destination.position(3);
    ByteBuffer slice = destination.slice();
    ByteBufferUtils.copy(source, 0, slice, 0, 3);
    assertArrayEquals(new byte[] {0, 0, 0, 1, 2, 3}, destination.array());
    assertEquals(0, source.position());
    assertEquals(0, slice.position());
  }

  @Test
  public void rangeCopyPreservesPositionLimitAndMark() {
    ByteBuffer source = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5});
    source.position(3).mark();
    ByteBuffer result = ByteBufferUtils.copy(source, 0, 2);
    assertArrayEquals(new byte[] {1, 2}, result.array());
    assertEquals(3, source.position());
    assertEquals(5, source.limit());
    source.reset();
    assertEquals(3, source.position());
  }

  @Test
  public void directAndReadOnlySourcesSupportOverlappingCopy() {
    ByteBuffer direct = ByteBuffer.allocateDirect(5);
    direct.put(new byte[] {1, 2, 3, 4, 5}).flip();
    ByteBuffer source = direct.asReadOnlyBuffer();
    ByteBufferUtils.copy(source, 0, direct, 1, 4);
    byte[] actual = new byte[5];
    direct.duplicate().get(actual);
    assertArrayEquals(new byte[] {1, 1, 2, 3, 4}, actual);
    assertEquals(0, direct.position());
    assertEquals(0, source.position());
  }

  @Test
  public void invalidRangesDoNotMutateBuffers() {
    ByteBuffer source = ByteBuffer.wrap(new byte[] {1, 2, 3});
    source.position(1).mark();
    try {
      ByteBufferUtils.copy(source, 0, 4);
      fail("Expected invalid range");
    } catch (IllegalArgumentException | IndexOutOfBoundsException expected) {
      assertEquals(1, source.position());
      assertEquals(3, source.limit());
      source.reset();
    }
    try {
      ByteBufferUtils.copy(source, 0, source.asReadOnlyBuffer(), 0, 1);
      fail("Expected read-only destination error");
    } catch (ReadOnlyBufferException expected) {
      assertArrayEquals(new byte[] {1, 2, 3}, source.array());
    }
  }
}
