package com.litongjava.tio.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



import com.litongjava.model.sys.SysConst;

/**
 * this class copied from org.apache.commons.io.IOUtils
 */
public class IoUtils {
  public static final int EOF = -1;
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  public static long copyLarge(final InputStream input, final OutputStream output, final byte[] buffer)
      throws IOException {
    long count = 0;
    int n;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  public static byte[] toByteArray(final InputStream input) throws IOException {
    try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      copy(input, output);
      return output.toByteArray();
    }
  }

  public static long copy(final InputStream input, final OutputStream output, final int bufferSize) throws IOException {
    return copyLarge(input, output, new byte[bufferSize]);
  }

  public static long copyLarge(final InputStream input, final OutputStream output) throws IOException {
    return copy(input, output, DEFAULT_BUFFER_SIZE);
  }

  public static int copy(final InputStream input, final OutputStream output) throws IOException {
    final long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  public static String streamToString(InputStream inputStream) {
    try {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
        result.write(buffer, 0, length);
      }
      return result.toString(SysConst.DEFAULT_ENCODING);
    } catch (Exception e) {
      return null;
    }
  }

  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
