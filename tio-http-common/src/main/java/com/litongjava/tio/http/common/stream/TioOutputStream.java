package com.litongjava.tio.http.common.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.litongjava.aio.BytePacket;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.encoder.ChunkEncoder;
import com.litongjava.tio.http.common.sse.ChunkedPacket;

public class TioOutputStream extends OutputStream {
  private final ChannelContext ctx;
  private final boolean chunked;
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  public TioOutputStream(ChannelContext ctx, boolean chunked) {
    this.ctx = ctx;
    this.chunked = chunked;
  }

  @Override
  public void write(int b) throws IOException {
    baos.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    baos.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    byte[] data = baos.toByteArray();
    if (data.length > 0) {
      if (chunked) {
        byte[] chunk = ChunkEncoder.encodeChunk(data);
        Tio.bSend(ctx, new ChunkedPacket(chunk));
      } else {
        Tio.bSend(ctx, new BytePacket(data));
      }
      baos.reset();  // **clear after sending**
    }
  }

  @Override
  public void close() throws IOException {
    // 1. Flush any remaining data
    flush();

    if (chunked) {
      // 2. Send the zero-length chunk to terminate the stream
      byte[] endChunk = ChunkEncoder.encodeChunk(new byte[0]);
      Tio.bSend(ctx, new ChunkedPacket(endChunk));
    }

    // 3. Close the connection
    Tio.close(ctx, "close");
  }
}
