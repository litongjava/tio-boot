package com.litongjava.tio.http.common.encoder;

import java.nio.charset.StandardCharsets;

public class ChunkEncoder {
  public static byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

  public static byte[] encodeChunk(byte[] data) {
    return encodeChunk(data, data.length);
  }

  public static byte[] encodeChunk(byte[] chunkData, int length) {
    String chunkSize = Integer.toHexString(length);
    byte[] chunkSizeBytes = (chunkSize + "\r\n").getBytes(StandardCharsets.UTF_8);

    byte[] chunk = new byte[chunkSizeBytes.length + length + CRLF.length];

    System.arraycopy(chunkSizeBytes, 0, chunk, 0, chunkSizeBytes.length);
    System.arraycopy(chunkData, 0, chunk, chunkSizeBytes.length, length);
    System.arraycopy(CRLF, 0, chunk, chunkSizeBytes.length + length, CRLF.length);

    return chunk;
  }
}
