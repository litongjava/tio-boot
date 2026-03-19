package com.litongjava.tio.core.task;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.LockSupport;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.enhance.channel.EnhanceAsynchronousSocketChannel;
import com.litongjava.tio.core.ChannelCloseCode;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.WriteCompletionHandler;
import com.litongjava.tio.core.intf.AioHandler;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.core.ssl.SslUtils;
import com.litongjava.tio.core.ssl.SslVo;
import com.litongjava.tio.core.utils.TioUtils;
import com.litongjava.tio.core.vo.WriteCompletionVo;

/**
 * Send data to client
 * 
 * @author Tong Li
 */
public class SendPacketTask {
  private static final Logger log = LoggerFactory.getLogger(SendPacketTask.class);
  private final static boolean disgnostic = TioConfig.disgnostic;

  public boolean canSend = true;
  private ChannelContext channelContext = null;
  private TioConfig tioConfig = null;
  private AioHandler aioHandler = null;
  private boolean isSsl = false;

  public SendPacketTask(ChannelContext channelContext) {
    this.channelContext = channelContext;
    this.tioConfig = channelContext.tioConfig;
    this.aioHandler = tioConfig.getAioHandler();
    this.isSsl = SslUtils.isSsl(tioConfig);
  }

  private ByteBuffer getByteBuffer(Packet packet) {
    ByteBuffer byteBuffer = packet.getPreEncodedByteBuffer();
    try {
      if (byteBuffer == null) {
        byteBuffer = aioHandler.encode(packet, tioConfig, channelContext);
      }
      if (!byteBuffer.hasRemaining()) {
        byteBuffer.flip();
      }
      return byteBuffer;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean sendPacket(Packet packet) {
    if (disgnostic) {
      log.info("send:{},{}", channelContext.getClientNode(), packet);
    }
    // 将数据包加入队列
    channelContext.sendQueue.offer(packet);
    // 如果当前没有发送且队列不为空，则开始发送
    if (channelContext.isSending.compareAndSet(false, true)) {
      Packet nextPacket = channelContext.sendQueue.poll();
      if (nextPacket != null) {
        ByteBuffer byteBuffer = getByteBuffer(nextPacket);
        if (isSsl) {
          if (!nextPacket.isSslEncrypted()) {
            SslVo sslVo = new SslVo(byteBuffer, nextPacket);
            try {
              channelContext.sslFacadeContext.getSslFacade().encrypt(sslVo);
              byteBuffer = sslVo.getByteBuffer();
            } catch (SSLException e) {
              log.error(channelContext.toString() + ", An exception occurred while performing SSL encryption", e);
              Tio.close(channelContext, "An exception occurred during SSL encryption.",
                  ChannelCloseCode.SSL_ENCRYPTION_ERROR);
              return false;
            }
          }
        }

        AsynchronousSocketChannel asc = channelContext.asynchronousSocketChannel;
        File fileBody = nextPacket.getFileBody();
        if (fileBody != null && asc instanceof EnhanceAsynchronousSocketChannel) {
          SocketChannel sc = ((EnhanceAsynchronousSocketChannel) asc).getSocketChannel();

          try {
            writeFully(sc, byteBuffer); // 先确保 header 发完
            transfer(fileBody, nextPacket, asc); // 再发 body
          } catch (IOException e) {
            log.error("send file header error, channel: {}", channelContext, e);
            Tio.close(channelContext, "send file header error");
            return false;
          }

          if (!nextPacket.isKeepConnection()) {
            Tio.close(channelContext, "Send file finish");
          }
        } else {
          sendByteBuffer(byteBuffer, nextPacket);
        }
      } else {
        channelContext.isSending.set(false);
      }
    }

    return true;
  }

  private void transfer(File fileBody, Packet nextPacket, AsynchronousSocketChannel asc) {
    SocketChannel sc = ((EnhanceAsynchronousSocketChannel) asc).getSocketChannel();

    long start = nextPacket.getFileBodyStart();
    long length = nextPacket.getFileBodyLength();
    long transferred = nextPacket.getFileBodyTransferred();

    if (!isSsl) {
      try (FileChannel fc = FileChannel.open(fileBody.toPath(), StandardOpenOption.READ)) {
        long fileSize = fc.size();

        if (start < 0 || start > fileSize) {
          log.error("invalid fileBodyStart: {}, fileSize: {}", start, fileSize);
          return;
        }

        if (length < 0) {
          length = fileSize - start;
        }

        long endExclusive = start + length;
        if (endExclusive > fileSize) {
          length = fileSize - start;
        }

        int idleRounds = 0;
        final int MAX_SPIN = 16;
        long backoffNanos = 1_000L;
        final long MAX_BACKOFF_NANOS = 1_000_000L;

        while (transferred < length && TioUtils.checkBeforeIO(channelContext)) {
          if (!sc.isOpen()) {
            break;
          }

          long position = start + transferred;
          long remaining = length - transferred;

          long sent = fc.transferTo(position, remaining, sc);
          if (sent > 0) {
            transferred += sent;
            nextPacket.setFileBodyTransferred(transferred);

            idleRounds = 0;
            backoffNanos = 1_000L;
          } else {
            idleRounds++;
            if (idleRounds <= MAX_SPIN) {
              LockSupport.parkNanos(backoffNanos);
              backoffNanos = Math.min(backoffNanos << 1, MAX_BACKOFF_NANOS);
            } else {
              LockSupport.parkNanos(MAX_BACKOFF_NANOS);
            }
          }
        }
      } catch (IOException e) {
        String msg = e.getMessage();

        if (e instanceof AsynchronousCloseException) {
          if (log.isDebugEnabled()) {
            log.debug("client closed connection during zero-copy, channel: {}", channelContext);
          }
        } else if (msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset by peer"))) {
          if (log.isDebugEnabled()) {
            log.debug("client closed connection during zero-copy, channel: {}", channelContext);
          }
        } else {
          log.error("zero-copy transfer file error, channel: {}", channelContext, e);
        }
      }
    } else {
      try (FileChannel fc = FileChannel.open(fileBody.toPath(), StandardOpenOption.READ)) {
        long fileSize = fc.size();

        if (length < 0) {
          length = fileSize - start;
        }

        fc.position(start);

        ByteBuffer buf = BufferPoolUtils.allocate(TioConfig.WRITE_CHUNK_SIZE, 64 * 1024);
        try {
          long remaining = length;
          while (remaining > 0 && TioUtils.checkBeforeIO(channelContext)) {
            buf.clear();
            int maxRead = (int) Math.min(buf.capacity(), remaining);
            buf.limit(maxRead);

            int readBytes = fc.read(buf);
            if (readBytes == -1) {
              break;
            }
            if (readBytes == 0) {
              continue;
            }

            remaining -= readBytes;
            transferred += readBytes;
            nextPacket.setFileBodyTransferred(transferred);

            buf.flip();

            SslVo sslVo = new SslVo(buf, nextPacket);
            try {
              channelContext.sslFacadeContext.getSslFacade().encrypt(sslVo);
            } catch (SSLException e) {
              log.error("Failed to encrypt data using ssl", e);
              Tio.close(channelContext, "Failed to encrypt data using ssl", ChannelCloseCode.SSL_ENCRYPTION_ERROR);
              break;
            }

            ByteBuffer encrypted = sslVo.getByteBuffer();
            while (encrypted.hasRemaining()) {
              sc.write(encrypted);
            }
          }
        } finally {
          BufferPoolUtils.clean(buf);
        }
      } catch (IOException e1) {
        log.error("ssl file transfer error, channel: {}", channelContext, e1);
      }
    }
  }

  /**
   *
   * @param byteBuffer
   * @param packets    Packet or List<Packet>
   * @author tanyaowu
   */
  private void sendByteBuffer(ByteBuffer byteBuffer, Object packets) {
    if (byteBuffer == null) {
      log.error("{},byteBuffer is null", channelContext);
      return;
    }
    if (!TioUtils.checkBeforeIO(channelContext)) {
      return;
    }

    // WriteCompletionVo：支持 returnToPool 参数
    WriteCompletionVo writeCompletionVo = new WriteCompletionVo(byteBuffer, packets);
    WriteCompletionHandler writeCompletionHandler = new WriteCompletionHandler(this.channelContext);
    this.channelContext.asynchronousSocketChannel.write(byteBuffer, writeCompletionVo, writeCompletionHandler);
  }

  public void processSendQueue() {
    // 如果当前没有发送且队列不为空，则开始发送
    if (channelContext.isSending.compareAndSet(false, true)) {
      Packet nextPacket = channelContext.sendQueue.poll();
      if (nextPacket != null) {
        ByteBuffer byteBuffer = getByteBuffer(nextPacket);
        if (isSsl) {
          if (!nextPacket.isSslEncrypted()) {
            SslVo sslVo = new SslVo(byteBuffer, nextPacket);
            try {
              channelContext.sslFacadeContext.getSslFacade().encrypt(sslVo);
              byteBuffer = sslVo.getByteBuffer();
            } catch (SSLException e) {
              log.error(channelContext.toString() + ", An exception occurred while performing SSL encryption", e);
              Tio.close(channelContext, "An exception occurred during SSL encryption.",
                  ChannelCloseCode.SSL_ENCRYPTION_ERROR);
            }
          }
        }
        sendByteBuffer(byteBuffer, nextPacket);
      } else {
        channelContext.isSending.set(false);
      }
    }
  }

  private void writeFully(SocketChannel sc, ByteBuffer buffer) throws IOException {
    while (buffer.hasRemaining()) {
      sc.write(buffer);
    }
  }
}