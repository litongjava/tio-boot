package com.litongjava.tio.utils.hutool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.litongjava.tio.utils.stream.TioGZIPInputStream;

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
   * Gzip解压处理
   * 
   * @param input 被解压的字节流
   * @return 解压后的字节流
   * @throws UtilException IO异常
   */
  /**
   * Gzip解压处理
   * @param input 被解压的字节流
   */
  public static byte[] unGzip(byte[] input) {
    try (FastByteArrayOutputStream bos = new FastByteArrayOutputStream(input.length)) {
      try (TioGZIPInputStream gis = new TioGZIPInputStream(new ByteArrayInputStream(input))) {
        byte[] buffer = new byte[input.length];
        int len;
        while ((len = gis.read(buffer)) != -1) {
          bos.write(buffer, 0, len);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return bos.toByteArray();
    }
  }
}
