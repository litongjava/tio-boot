package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import org.testng.annotations.Test;

public class BufferAppendBoundaryTest {
  private static class BoundedBuffer extends FastByteBuffer {
    private int appends;
    BoundedBuffer() {
      super(2);
    }
    @Override
    public FastByteBuffer append(byte[] bytes, int offset, int length) {
      // Bound the old self-append loop without allocating unbounded memory.
      if (++appends > 20) {
        throw new AssertionError("Self-append did not terminate");
      }
      return super.append(bytes, offset, length);
    }
  }

  @Test
  public void selfAppendAcrossChunksDuplicatesOriginalOnce() {
    BoundedBuffer buffer = new BoundedBuffer();
    for (int i = 1; i <= 5; i++) {
      buffer.append((byte) i);
    }
    assertSame(buffer, buffer.append(buffer));
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 1, 2, 3, 4, 5}, buffer.toArray());
  }

  @Test
  public void selfAppendWithinOneChunkAndEmptyBufferWorks() {
    FastByteBuffer buffer = new FastByteBuffer(16);
    buffer.append(buffer);
    assertEquals(0, buffer.size());
    buffer.append(new byte[] {1, 2, 3});
    buffer.append(buffer);
    assertArrayEquals(new byte[] {1, 2, 3, 1, 2, 3}, buffer.toArray());
  }

  @Test
  public void overflowedSourceRangeDoesNotAllocateOrChangeState() {
    FastByteBuffer buffer = new FastByteBuffer(2);
    try {
      buffer.append(new byte[1], Integer.MAX_VALUE, 1);
      fail("Invalid source range must be rejected");
    } catch (IndexOutOfBoundsException expected) {
      assertEquals(-1, buffer.index());
      assertEquals(0, buffer.size());
      assertEquals(0, buffer.offset());
    }
    buffer.append((byte) 7);
    assertArrayEquals(new byte[] {7}, buffer.toArray());
  }

  @Test
  public void resetReleasesAllPreviouslyUsedChunks() {
    FastByteBuffer buffer = new FastByteBuffer(2);
    for (int i = 0; i < 7; i++) {
      buffer.append((byte) i);
    }
    int last = buffer.index();
    buffer.reset();
    assertEquals(-1, buffer.index());
    assertEquals(0, buffer.size());
    for (int i = 0; i <= last; i++) {
      assertNull(buffer.array(i));
    }
    buffer.append((byte) 9);
    assertArrayEquals(new byte[] {9}, buffer.toArray());
  }

  @Test
  public void appendingAnotherBufferPreservesSourceAndOrder() {
    FastByteBuffer source = new FastByteBuffer(2);
    for (int i = 1; i <= 5; i++) {
      source.append((byte) i);
    }
    FastByteBuffer target = new FastByteBuffer(3);
    target.append((byte) 0);
    target.append(source);
    assertArrayEquals(new byte[] {0, 1, 2, 3, 4, 5}, target.toArray());
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, source.toArray());
  }
}
