package nexus.io.tio.utils;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.testng.annotations.Test;

public class IoBoundaryTest {
  @Test
  public void nonPositiveBufferSizesAreRejected() throws Exception {
    for (int size : new int[] {0, -1}) {
      try {
        IoUtils.copy(new ByteArrayInputStream(new byte[] {1}), new ByteArrayOutputStream(), size);
        fail("Expected an invalid buffer size error");
      } catch (IllegalArgumentException expected) {
        // Validate before allocating or reading.
      }
    }
  }

  @Test
  public void emptyBufferIsRejectedBeforeReading() throws Exception {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1}) {
      @Override
      public synchronized int read(byte[] buffer, int offset, int length) {
        fail("The invalid buffer must be rejected before starting the copy");
        return -1;
      }
    };
    try {
      IoUtils.copyLarge(input, new ByteArrayOutputStream(), new byte[0]);
      fail("Expected an invalid buffer error");
    } catch (IllegalArgumentException expected) {
      assertEquals(1, input.available());
    }
  }

  @Test
  public void copyPreservesBytesAndLeavesCallerStreamsOpen() throws Exception {
    byte[] bytes = {0, 1, -1, 7, 9};
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    assertEquals(bytes.length, IoUtils.copy(input, output, 2));
    assertArrayEquals(bytes, output.toByteArray());
    output.write(2);
    assertEquals(6, output.size());
  }
}
