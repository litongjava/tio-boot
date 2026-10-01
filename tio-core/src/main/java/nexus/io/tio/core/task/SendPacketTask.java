package nexus.io.tio.core.task;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import nexus.io.enhance.buffer.GlobalScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.aio.Packet;
import nexus.io.enhance.channel.EnhanceAsynchronousSocketChannel;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.WriteCompletionHandler;
import nexus.io.tio.core.pool.BufferPoolUtils;
import nexus.io.tio.core.ssl.SslUtils;
import nexus.io.tio.core.ssl.SslVo;
import nexus.io.tio.core.vo.WriteCompletionVo;

/** Serializes every packet through the same asynchronous send lifecycle. */
public class SendPacketTask {
  private static final Logger log = LoggerFactory.getLogger(SendPacketTask.class);
  private static final int FILE_CHUNK_SIZE = 64 * 1024;
  private final ChannelContext channelContext;
  private final AtomicInteger work = new AtomicInteger();
  private Operation current;
  private boolean closed;
  public boolean canSend = true;

  public SendPacketTask(ChannelContext channelContext) { this.channelContext = channelContext; }

  public boolean sendPacket(Packet packet) {
    return channelContext.getSendPacketTask().enqueue(packet);
  }

  private boolean enqueue(Packet packet) {
    synchronized (this) {
      // Client contexts can be reused after a completed close and reconnect.
      if (closed && current == null && !channelContext.isClosed && !channelContext.isRemoved && !channelContext.isWaitingClose) {
        closed = false;
      }
      if (closed || channelContext.isClosed || channelContext.isRemoved || channelContext.isWaitingClose) {
        notifyDiscarded(packet);
        return false;
      }
      channelContext.sendQueue.offer(packet);
    }
    drain();
    synchronized (this) { return !closed; }
  }

  public void processSendQueue() { channelContext.getSendPacketTask().drain(); }

  /** Pending buffers remain owned by their IO callback until the socket completes them. */
  public void connectionClosed() {
    SendPacketTask owner = channelContext.getSendPacketTask();
    if (owner != this) { owner.connectionClosed(); return; }
    synchronized (this) {
      closed = true;
      Packet packet;
      while ((packet = channelContext.sendQueue.poll()) != null) notifyDiscarded(packet);
      if (current != null && current.failure == null) current.failure = new ClosedChannelException();
    }
    drain();
  }

  private void notifyDiscarded(Packet packet) {
    try {
      new WriteCompletionHandler(channelContext).handleOne(0, new ClosedChannelException(), packet, false);
    } catch (Throwable error) {
      log.error("Failed to notify a discarded send", error);
    }
  }

  /** Trampolines inline completions to avoid recursive sends and lost queue wakeups. */
  private void drain() {
    if (work.getAndIncrement() != 0) return;
    int missed = 1;
    do {
      synchronized (this) { advance(); }
      missed = work.addAndGet(-missed);
    } while (missed != 0);
  }

  private void advance() {
    if (channelContext.isWaitingClose || channelContext.isClosed || channelContext.isRemoved) {
      closed = true;
      Packet discarded;
      while ((discarded = channelContext.sendQueue.poll()) != null) notifyDiscarded(discarded);
      if (current != null && current.failure == null) current.failure = new ClosedChannelException();
    }
    if (current != null && current.pending) return;
    if (current != null && current.retry != null) {
      if (!closed) return;
      current.retry.cancel(false);
      current.retry = null;
    }
    if (current == null) {
      if (closed) { channelContext.isSending.set(false); return; }
      Packet packet = channelContext.sendQueue.poll();
      if (packet == null) { channelContext.isSending.set(false); return; }
      channelContext.isSending.set(true);
      current = new Operation(packet);
      try { current.initialize(); }
      catch (Throwable error) { current.failure = error; }
    }
    Operation op = current;
    try {
      if (op.failure != null) { finish(op); return; }
      if (op.buffer == null || !op.buffer.hasRemaining()) {
        op.releaseBuffer();
        if (op.chunkBytes != 0) {
          op.transferred += op.chunkBytes;
          op.packet.setFileBodyTransferred(op.transferred);
          op.chunkBytes = 0;
        }
        if (op.file == null || op.transferred == op.fileLength) { finish(op); return; }
        if (!SslUtils.isSsl(channelContext.tioConfig)
            && channelContext.asynchronousSocketChannel instanceof EnhanceAsynchronousSocketChannel) {
          op.pending = true;
          ((EnhanceAsynchronousSocketChannel) channelContext.asynchronousSocketChannel).transfer(
              op.file, op.fileStart + op.transferred, op.fileLength - op.transferred, op,
              new CompletionHandler<Long, Operation>() {
                @Override public void completed(Long count, Operation attachment) {
                  synchronized (SendPacketTask.this) {
                    if (current != attachment || !attachment.pending) return;
                    attachment.pending = false;
                    if (count == null || count <= 0) attachment.failure = new IOException("File transfer made no progress");
                    else {
                      attachment.transferred += count;
                      attachment.written += count;
                      attachment.packet.setFileBodyTransferred(attachment.transferred);
                    }
                  }
                  drain();
                }
                @Override public void failed(Throwable error, Operation attachment) {
                  synchronized (SendPacketTask.this) {
                    if (current != attachment || !attachment.pending) return;
                    attachment.pending = false;
                    attachment.failure = error;
                  }
                  drain();
                }
              });
          return;
        }
        op.readChunk();
      }
      op.pending = true;
      channelContext.asynchronousSocketChannel.write(op.buffer, op, new CompletionHandler<Integer, Operation>() {
        @Override public void completed(Integer count, Operation attachment) {
          synchronized (SendPacketTask.this) {
            if (current != attachment || !attachment.pending) return;
            attachment.pending = false;
            if (count == null || count < 0) attachment.failure = new IOException("Invalid write result: " + count);
            else {
              attachment.written += count;
              if (count == 0 && attachment.buffer.hasRemaining()) {
                // Some providers complete zero-byte writes inline; retry without spinning.
                try {
                  attachment.retry = GlobalScheduler.INSTANCE.schedule(() -> {
                    synchronized (SendPacketTask.this) { attachment.retry = null; }
                    drain();
                  }, 10, TimeUnit.MILLISECONDS);
                } catch (RuntimeException error) {
                  attachment.failure = error;
                }
              }
            }
          }
          drain();
        }
        @Override public void failed(Throwable error, Operation attachment) {
          synchronized (SendPacketTask.this) {
            if (current != attachment || !attachment.pending) return;
            attachment.pending = false;
            attachment.failure = error;
          }
          drain();
        }
      });
    } catch (Throwable error) {
      op.pending = false;
      op.failure = error;
      finish(op);
    }
  }

  private void finish(Operation op) {
    if (current != op) return;
    current = null;
    op.releaseBuffer();
    if (op.file != null) {
      try { op.file.close(); } catch (IOException error) { if (op.failure == null) op.failure = error; }
    }
    try {
      int count = (int) Math.min(Integer.MAX_VALUE, op.written);
      new WriteCompletionHandler(channelContext).handle(count, op.failure, new WriteCompletionVo(null, op.packet));
    } catch (Throwable error) {
      log.error("Failed to finish a send", error);
      Tio.close(channelContext, error, "Send completion failed");
    } finally {
      if (op.failure != null) Tio.close(channelContext, op.failure, "Send failed");
      work.incrementAndGet();
    }
  }

  private final class Operation {
    final Packet packet;
    ByteBuffer buffer;
    boolean owned;
    boolean pending;
    ScheduledFuture<?> retry;
    Throwable failure;
    FileChannel file;
    long fileStart;
    long fileLength;
    long transferred;
    long written;
    int chunkBytes;

    Operation(Packet packet) { this.packet = packet; }

    void initialize() throws Exception {
      ByteBuffer preEncoded = packet.getPreEncodedByteBuffer();
      buffer = preEncoded == null ? channelContext.tioConfig.getAioHandler().encode(packet, channelContext.tioConfig, channelContext)
          : preEncoded.duplicate();
      owned = preEncoded == null;
      if (buffer == null) throw new IOException("Packet encoder returned null");
      if (!buffer.hasRemaining()) buffer.flip();
      // Encoding may clear the file body for HEAD or bodyless HTTP responses.
      if (packet.getFileBody() != null) {
        file = FileChannel.open(packet.getFileBody().toPath(), StandardOpenOption.READ);
        fileStart = packet.getFileBodyStart();
        long size = file.size();
        if (fileStart < 0 || fileStart > size) throw new IOException("Invalid file range start");
        fileLength = packet.getFileBodyLength();
        if (fileLength < 0) fileLength = size - fileStart;
        if (fileLength > size - fileStart) throw new IOException("File range exceeds the available body");
        transferred = packet.getFileBodyTransferred();
        if (transferred < 0 || transferred > fileLength) throw new IOException("Invalid file transfer offset");
      }
      encrypt();
    }

    void readChunk() throws Exception {
      buffer = BufferPoolUtils.allocate(FILE_CHUNK_SIZE);
      owned = true;
      buffer.limit((int) Math.min(FILE_CHUNK_SIZE, fileLength - transferred));
      int count = file.read(buffer, fileStart + transferred);
      if (count <= 0) throw new IOException("File body ended before the advertised length");
      chunkBytes = count;
      buffer.flip();
      encrypt();
    }

    void encrypt() throws Exception {
      if (!SslUtils.isSsl(channelContext.tioConfig) || packet.isSslEncrypted()) return;
      ByteBuffer plain = buffer;
      boolean plainOwned = owned;
      SslVo ssl = new SslVo(plain, packet);
      channelContext.sslFacadeContext.getSslFacade().encrypt(ssl);
      ByteBuffer encrypted = ssl.getByteBuffer();
      if (encrypted == null) throw new IOException("SSL encoder returned null");
      if (encrypted != plain) {
        if (plainOwned) BufferPoolUtils.clean(plain);
        buffer = encrypted;
        owned = true;
      }
    }

    void releaseBuffer() {
      if (owned && buffer != null) BufferPoolUtils.clean(buffer);
      buffer = null;
      owned = false;
    }
  }
}
