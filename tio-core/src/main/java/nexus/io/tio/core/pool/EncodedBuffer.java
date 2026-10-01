package nexus.io.tio.core.pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntFunction;
import nexus.io.enhance.buffer.VirtualBuffer;
import nexus.io.tio.core.TioConfig;

/** A send buffer and its release action, owned by one asynchronous send operation. */
public final class EncodedBuffer implements AutoCloseable {
  private final ByteBuffer buffer;
  private final Runnable release;
  private final AtomicBoolean closed = new AtomicBoolean();

  private EncodedBuffer(ByteBuffer buffer, Runnable release) {
    this.buffer = Objects.requireNonNull(buffer, "buffer");
    this.release = release;
  }

  /** Allocates from the existing response VirtualBuffer pool. */
  public static EncodedBuffer allocate(int size) {
    if (size < 0) throw new IllegalArgumentException("Negative buffer size");
    VirtualBuffer owner = BufferPoolUtils.allocateResponse(TioConfig.WRITE_CHUNK_SIZE, Math.max(1, size));
    owner.buffer().order(ByteOrder.BIG_ENDIAN).limit(size);
    return new EncodedBuffer(owner.buffer(), owner::clean);
  }

  /** Transfers ownership of a VirtualBuffer to a send operation. */
  public static EncodedBuffer owned(VirtualBuffer owner) {
    Objects.requireNonNull(owner, "owner");
    return new EncodedBuffer(owner.buffer(), owner::clean);
  }

  /** Compatibility for encoders that return an owned ordinary ByteBuffer. */
  public static EncodedBuffer owned(ByteBuffer buffer) {
    return new EncodedBuffer(buffer, () -> BufferPoolUtils.clean(buffer));
  }

  /** Shares readable bytes without transferring their storage ownership. */
  public static EncodedBuffer borrowed(ByteBuffer buffer) {
    return new EncodedBuffer(buffer.duplicate(), null);
  }

  /** Encodes into one pooled output, releasing it if encoding fails. */
  public static EncodedBuffer encodePooled(Function<IntFunction<ByteBuffer>, ByteBuffer> encoder) {
    EncodedBuffer[] allocated = new EncodedBuffer[1];
    try {
      ByteBuffer result = encoder.apply(size -> {
        if (allocated[0] != null) throw new IllegalStateException("Encoder allocated more than one output");
        allocated[0] = allocate(size);
        return allocated[0].buffer();
      });
      if (allocated[0] == null || result != allocated[0].buffer())
        throw new IllegalStateException("Encoder must return its allocated output");
      return allocated[0];
    } catch (RuntimeException | Error error) {
      if (allocated[0] != null) allocated[0].close();
      throw error;
    }
  }

  public ByteBuffer buffer() { return buffer; }

  @Override public void close() {
    if (closed.compareAndSet(false, true) && release != null) release.run();
  }
}
