package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.testng.annotations.Test;
import nexus.io.tio.utils.base64.Base64Utils;
import nexus.io.tio.utils.encoder.ImageVo;
import nexus.io.tio.utils.stream.TioGZIPInputStream;

public class StreamImageTraversalTest {
  private byte[] readAll(TioGZIPInputStream stream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[3];
    int size;
    while ((size = stream.read(buffer)) != -1) {
      assertTrue("A blocking read must make progress", size > 0);
      result.write(buffer, 0, size);
    }
    return result.toByteArray();
  }
  @Test public void gzipReadsWhenAvailableIsZero() throws Exception {
    byte[] bytes = ZipUtil.gzip(new byte[] {65, 66, 67});
    InputStream source = new ByteArrayInputStream(bytes) {
      @Override public int available() { return 0; }
      @Override public synchronized int read(byte[] b, int off, int len) {
        return super.read(b, off, Math.min(len, 1));
      }
    };
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(source)) {
      assertArrayEquals(new byte[] {65, 66, 67}, readAll(gzip));
    }
  }
  @Test public void singleByteReadDoesNotInventData() throws Exception {
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(new byte[0]))) {
      try { gzip.read(); fail("Expected truncated header"); } catch (EOFException expected) { }
    }
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(ZipUtil.gzip(new byte[] {0, -1})))) {
      assertEquals(0, gzip.read()); assertEquals(255, gzip.read()); assertEquals(-1, gzip.read());
    }
  }
  @Test public void zeroLengthAndInvalidReadsDoNotConsumeInput() throws Exception {
    ByteArrayInputStream source = new ByteArrayInputStream(ZipUtil.gzip(new byte[] {1}));
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(source)) {
      int available = source.available();
      assertEquals(0, gzip.read(new byte[0], 0, 0));
      assertEquals(available, source.available());
      try { gzip.read(null, 0, 1); fail(); } catch (NullPointerException expected) { }
      try { gzip.read(new byte[1], 2, 0); fail(); } catch (IndexOutOfBoundsException expected) { }
      assertEquals(available, source.available());
      assertArrayEquals(new byte[] {1}, readAll(gzip));
      assertEquals(0, gzip.read(new byte[0], 0, 0));
    }
  }
  @Test public void truncatedTrailerAndBadChecksumAreRejected() throws Exception {
    byte[] bytes = ZipUtil.gzip(new byte[] {1, 2});
    byte[] corrupt = bytes.clone(); corrupt[corrupt.length - 8] ^= 1;
    for (byte[] input : new byte[][] {Arrays.copyOf(bytes, bytes.length - 1), corrupt}) {
      try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(input))) {
        try { readAll(gzip); fail("Expected invalid gzip"); } catch (IOException expected) { }
      }
    }
  }
  @Test public void imageDataUrlRejectsInvalidStructure() {
    for (String input : new String[] {null, "", "other:image/png;plain,QQ==", "data:image/png,QQ==",
        "data:image/png;base64,", "data:image/png;base64,QQ==,ignored", "data:image/png;base64,%%%%"}) {
      try { Base64Utils.decodeImage(input); fail("Invalid Data URL accepted: " + input); }
      catch (IllegalArgumentException expected) { }
    }
  }
  @Test public void imageDataUrlSupportsParametersAndRoundTrip() {
    byte[] bytes = {1, 2, 3};
    ImageVo image = Base64Utils.decodeImage(Base64Utils.encodeImage(bytes, "image/png"));
    assertEquals("image/png", image.getMimeType()); assertArrayEquals(bytes, image.getData());
    ImageVo parameterized = Base64Utils.decodeImage("data:image/png;charset=utf-8;base64,AQID");
    assertEquals("image/png", parameterized.getMimeType()); assertArrayEquals(bytes, parameterized.getData());
  }
  @Test public void directoryTraversalSkipsLinkedDirectoriesAndAppliesFilter() throws Exception {
    Path parent = Paths.get("target").toAbsolutePath().toRealPath();
    Path root = Files.createTempDirectory(parent, "traversal-test-");
    Path child = Files.createDirectory(root.resolve("child"));
    Path marker = Files.write(child.resolve("keep.txt"), new byte[] {1});
    Path link = child.resolve("back");
    try {
      Files.createSymbolicLink(link, root);
      assertEquals(Collections.singletonList(marker.toFile()), FileUtil.loopFiles(root.toFile(), file -> file.getName().endsWith(".txt")));
      assertTrue(FileUtil.loopFiles(link.toFile()).isEmpty());
      assertTrue(FileUtil.loopFiles(root.toFile(), file -> false).isEmpty());
      assertEquals(Collections.singletonList(marker.toFile()), FileUtil.loopFiles(marker.toFile()));
    } finally {
      // Each cleanup target was created directly in this fixture; do not follow the link.
      Files.deleteIfExists(link); Files.deleteIfExists(marker); Files.deleteIfExists(child); Files.deleteIfExists(root);
    }
  }
  @Test public void optionalHeaderFieldsAndHeaderChecksum() throws Exception {
    byte[] original = ZipUtil.gzip(new byte[] {4, 5, 6});
    ByteArrayOutputStream header = new ByteArrayOutputStream();
    byte[] fixed = Arrays.copyOf(original, 10);
    fixed[3] = 30;
    header.write(fixed);
    header.write(new byte[] {2, 0, 42, 43, 'n', 0, 'c', 0});
    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
    crc.update(header.toByteArray());
    int checksum = (int) crc.getValue();
    header.write(checksum & 255);
    header.write((checksum >>> 8) & 255);
    header.write(original, 10, original.length - 10);
    byte[] input = header.toByteArray();
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(input), 1)) {
      assertArrayEquals(new byte[] {4, 5, 6}, readAll(gzip));
    }
    input[18] ^= 1;
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(input))) {
      try { readAll(gzip); fail("Expected header checksum failure"); } catch (java.util.zip.ZipException expected) { }
    }
  }

  @Test public void everyTruncatedPrefixFailsAndEmptyMemberSucceeds() throws Exception {
    byte[] data = ZipUtil.gzip(new byte[] {1, 2, 3, 4});
    for (int length = 0; length < data.length; length++) {
      try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(Arrays.copyOf(data, length)))) {
        try { readAll(gzip); fail("Truncated input accepted at " + length); } catch (IOException expected) { }
      }
    }
    try (TioGZIPInputStream gzip = new TioGZIPInputStream(new ByteArrayInputStream(ZipUtil.gzip(new byte[0])))) {
      assertEquals(-1, gzip.read());
    }
  }

  @Test public void zeroBulkReadsMakeProgressAndCloseFailureStillCloses() throws Exception {
    InputStream source = new ByteArrayInputStream(ZipUtil.gzip(new byte[] {7, 8})) {
      @Override public synchronized int read(byte[] bytes, int offset, int length) { return 0; }
      @Override public void close() throws IOException { throw new IOException("close failure"); }
    };
    TioGZIPInputStream gzip = new TioGZIPInputStream(source);
    try {
      assertArrayEquals(new byte[] {7, 8}, readAll(gzip));
    } finally {
      try { gzip.close(); fail("Expected close failure"); } catch (IOException expected) { }
    }
    gzip.close();
    try { gzip.read(); fail("Closed stream accepted a read"); } catch (IOException expected) { }
  }

}
