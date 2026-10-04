package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class CompressionBufferBoundaryTest {
  @Test
  public void emptyAndResetOutputCanBeWritten() throws Exception {
    FastByteArrayOutputStream source = new FastByteArrayOutputStream();
    ByteArrayOutputStream target = new ByteArrayOutputStream();
    source.writeTo(target);
    source.write(7);
    source.reset();
    source.writeTo(target);
    assertEquals(0, target.size());
    source.write(9);
    source.writeTo(target);
    assertArrayEquals(new byte[] {9}, target.toByteArray());
  }

  @Test
  public void slicesRejectUnwrittenBytesAndInvalidRanges() {
    FastByteBuffer buffer = new FastByteBuffer(16);
    buffer.append(new byte[] {1, 2, 3});
    int[][] ranges = {{0, 4}, {3, 1}, {-1, 1}, {4, 0}, {0, -1}, {Integer.MAX_VALUE, 2}};
    for (int[] range : ranges) {
      try {
        buffer.toArray(range[0], range[1]);
        fail("Invalid logical range must be rejected");
      } catch (IndexOutOfBoundsException expected) {
        assertEquals(3, buffer.size());
      }
    }
    assertArrayEquals(new byte[0], buffer.toArray(3, 0));
  }

  @Test
  public void crossChunkSlicesAndWritesPreserveBytes() throws Exception {
    FastByteBuffer buffer = new FastByteBuffer(2);
    FastByteArrayOutputStream stream = new FastByteArrayOutputStream(2);
    for (int i = 0; i < 19; i++) {
      buffer.append((byte) i);
      stream.write(i);
    }
    assertArrayEquals(Arrays.copyOfRange(buffer.toArray(), 1, 18), buffer.toArray(1, 17));
    ByteArrayOutputStream target = new ByteArrayOutputStream();
    stream.writeTo(target);
    assertArrayEquals(buffer.toArray(), target.toByteArray());
  }

  @Test
  public void truncatedGzipTerminatesWithAnIoCause() throws Exception {
    String java = new File(System.getProperty("java.home"), "bin/java.exe").getAbsolutePath();
    Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
        getClass().getName()).redirectErrorStream(true).start();
    try {
      assertTrue("Truncated gzip must not spin indefinitely", process.waitFor(5, TimeUnit.SECONDS));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[512];
      int count;
      while ((count = process.getInputStream().read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
      assertEquals(output.toString("UTF-8"), 0, process.exitValue());
    } finally {
      process.destroyForcibly();
      process.waitFor(5, TimeUnit.SECONDS);
    }
  }

  public static void main(String[] args) {
    byte[] gzip = ZipUtil.gzip("payload".getBytes(StandardCharsets.UTF_8));
    int[] lengths = {0, 1, 9, gzip.length - 1};
    for (int length : lengths) {
      try {
        ZipUtil.unGzip(Arrays.copyOf(gzip, length));
        throw new AssertionError("Truncated gzip accepted");
      } catch (RuntimeException expected) {
        if (!(expected.getCause() instanceof IOException)) {
          throw new AssertionError("Expected an IOException cause", expected);
        }
      }
    }
  }

  @Test
  public void gzipRoundTripsEmptyAndBinaryData() {
    assertArrayEquals(new byte[0], ZipUtil.unGzip(ZipUtil.gzip(new byte[0])));
    byte[] data = new byte[32768];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) i;
    }
    assertArrayEquals(data, ZipUtil.unGzip(ZipUtil.gzip(data)));
  }

  @Test
  public void concatenatedGzipMembersAreDecoded() throws Exception {
    ByteArrayOutputStream compressed = new ByteArrayOutputStream();
    compressed.write(ZipUtil.gzip(new byte[] {1, 2}));
    compressed.write(ZipUtil.gzip(new byte[] {3, 4}));
    assertArrayEquals(new byte[] {1, 2, 3, 4}, ZipUtil.unGzip(compressed.toByteArray()));
  }

  @Test
  public void corruptGzipChecksumIsRejected() {
    byte[] compressed = ZipUtil.gzip(new byte[] {1, 2});
    compressed[compressed.length - 8] ^= 1;
    try {
      ZipUtil.unGzip(compressed);
      fail("Corrupt checksum must be rejected");
    } catch (RuntimeException expected) {
      assertTrue(expected.getCause() instanceof IOException);
    }
  }
}
