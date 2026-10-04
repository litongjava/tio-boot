package nexus.io.tio.utils.stream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * Blocking input stream for a single GZIP member, with lazy header parsing.
 * Use GZIPInputStream for concatenated members.
 * @author 三刀
 */
public class TioGZIPInputStream extends TioInflaterInputStream {
  protected CRC32 crc = new CRC32();
  protected boolean eos;
  public static final int GZIP_MAGIC = 0x8b1f;
  private boolean headerRead;
  private boolean closed;

  public TioGZIPInputStream(InputStream in, int size) throws IOException {
    super(in, createInflater(in, size), size);
    usesDefaultInflater = true;
  }

  public TioGZIPInputStream(InputStream in) throws IOException {
    this(in, 512);
  }

  private static Inflater createInflater(InputStream in, int size) {
    if (in == null) {
      throw new NullPointerException("Input stream");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("buffer size <= 0");
    }
    return new Inflater(true);
  }

  @Override
  public int read(byte[] destination, int offset, int length) throws IOException {
    if (closed) {
      throw new IOException("Stream closed");
    }
    if (destination == null) {
      throw new NullPointerException("Destination buffer");
    }
    if (offset < 0 || length < 0 || length > destination.length - offset) {
      throw new IndexOutOfBoundsException();
    }
    if (length == 0) {
      return 0;
    }
    if (eos) {
      return -1;
    }
    if (!headerRead) {
      readHeader();
      headerRead = true;
    }
    int count = super.read(destination, offset, length);
    if (count >= 0) {
      crc.update(destination, offset, count);
      return count;
    }
    readTrailer();
    eos = true;
    return -1;
  }

  private void readHeader() throws IOException {
    crc.reset();
    CheckedInputStream header = new CheckedInputStream(in, crc);
    if (readUShort(header) != GZIP_MAGIC) {
      throw new ZipException("Not in GZIP format");
    }
    if (readUByte(header) != 8) {
      throw new ZipException("Unsupported compression method");
    }
    int flags = readUByte(header);
    if ((flags & 0xe0) != 0) {
      throw new ZipException("Reserved GZIP flags are set");
    }
    skipBytes(header, 6);
    if ((flags & 4) != 0) {
      skipBytes(header, readUShort(header));
    }
    if ((flags & 8) != 0) {
      skipTerminated(header);
    }
    if ((flags & 16) != 0) {
      skipTerminated(header);
    }
    if ((flags & 2) != 0) {
      int expected = (int) crc.getValue() & 0xffff;
      if (readUShort(header) != expected) {
        throw new ZipException("Corrupt GZIP header");
      }
    }
    crc.reset();
  }

  private void readTrailer() throws IOException {
    int remaining = inf.getRemaining();
    // Reuse compressed input already buffered by the inflater before reading more.
    InputStream trailer = new SequenceInputStream(
        new ByteArrayInputStream(buf, len - remaining, remaining), in);
    long checksum = readUInt(trailer);
    long size = readUInt(trailer);
    if (checksum != crc.getValue() || size != (inf.getBytesWritten() & 0xffffffffL)) {
      throw new ZipException("Corrupt GZIP trailer");
    }
    // Preserve single-member behavior when additional input is already observable.
    if (remaining > 8 || in.available() > 0) {
      throw new java.io.UnsupportedEncodingException("Concatenated GZIP members are not supported");
    }
  }

  private static void skipTerminated(InputStream source) throws IOException {
    while (readUByte(source) != 0) {
      // Consume the complete zero-terminated header field.
    }
  }

  private static void skipBytes(InputStream source, int count) throws IOException {
    for (int i = 0; i < count; i++) {
      readUByte(source);
    }
  }

  private static int readUByte(InputStream source) throws IOException {
    int value = source.read();
    if (value < 0) {
      throw new EOFException("Unexpected end of GZIP input stream");
    }
    return value;
  }

  private static int readUShort(InputStream source) throws IOException {
    int low = readUByte(source);
    return low | (readUByte(source) << 8);
  }

  private static long readUInt(InputStream source) throws IOException {
    long low = readUShort(source);
    return low | ((long) readUShort(source) << 16);
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      eos = true;
      closed = true;
    }
  }
}
