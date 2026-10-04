package nexus.io.tio.utils.hutool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import java.util.zip.GZIPInputStream;

/**
 * Zip工具类
 * 
 * @author looly
 *
 */
public class ZipUtil {
  /**
   * Gzip压缩处理
   * 
   * @param input 被压缩的字节流
   * @return 压缩后的字节流
   */
  public static byte[] gzip(byte[] input) {
    FastByteArrayOutputStream bos = new FastByteArrayOutputStream(input.length);
    try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
      gos.write(input, 0, input.length);
      gos.finish();
      gos.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bos.toByteArray();
  }

  /**
   * Decompresses a complete GZIP byte array, including concatenated members.
   *
   * @param input complete compressed data
   * @return decompressed bytes
   * @throws RuntimeException if the input is truncated or corrupt, with the I/O cause
   */
  public static byte[] unGzip(byte[] input) {
    try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream(input.length);
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(input))) {
      // A complete byte array needs EOF detection rather than incremental polling.
      byte[] buffer = new byte[8192];
      int len;
      while ((len = gis.read(buffer)) != -1) {
        bos.write(buffer, 0, len);
      }
      return bos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
