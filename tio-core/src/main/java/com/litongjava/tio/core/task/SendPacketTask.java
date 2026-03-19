package com.litongjava.tio.core.task;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
              Tio.close(channelContext, "An exception occurred during SSL encryption.", ChannelCloseCode.SSL_ENCRYPTION_ERROR);
              return false;
            }
          }
        }

        AsynchronousSocketChannel asc = channelContext.asynchronousSocketChannel;
        File fileBody = packet.getFileBody();
        if (fileBody != null && asc instanceof EnhanceAsynchronousSocketChannel) {
          boolean keepConnection = nextPacket.isKeepConnection();
          // send header
          nextPacket.setKeepConnection(true);
          sendByteBuffer(byteBuffer, nextPacket);

          transfer(fileBody, nextPacket, asc);

          if (!keepConnection) {
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
    if (!isSsl) {
      // —— 零拷贝：加退避，避免死循环打满 CPU ——
      try (FileChannel fc = FileChannel.open(fileBody.toPath(), StandardOpenOption.READ)) {
        long pos = 0, size = fc.size();

        // 自旋次数 + 指数退避
        int idleRounds = 0;
        final int MAX_SPIN = 16; // 前几轮稍微积极一点
        long backoffNanos = 1_000L; // 初始 1 微秒
        final long MAX_BACKOFF_NANOS = 1_000_000L; // 最大退避到 1 毫秒

        while (pos < size && TioUtils.checkBeforeIO(channelContext)) {
          long sent = fc.transferTo(pos, size - pos, sc);
          if (sent > 0) {
            pos += sent;
            // 一旦写出成功，重置退避状态
            idleRounds = 0;
            backoffNanos = 1_000L;
          } else {
            // 写不动：说明内核缓冲区满了或对端太慢
            idleRounds++;
            if (idleRounds <= MAX_SPIN) {
              // 前几次：短暂退避 + 指数退避，兼顾吞吐和延迟
              LockSupport.parkNanos(backoffNanos);
              backoffNanos = Math.min(backoffNanos << 1, MAX_BACKOFF_NANOS);
            } else {
              // 再不行就稳定按最大退避，防止占死一个 CPU 核
              LockSupport.parkNanos(MAX_BACKOFF_NANOS);
            }
          }
        }
      } catch (IOException e) {
        log.error("zero-copy transfer file error, channel: {}", channelContext, e);
      }
    } else {
      // —— SSL 分支暂时保持原样（下方可以再一起优化） ——
      try (FileChannel fc = FileChannel.open(fileBody.toPath(), StandardOpenOption.READ)) {
        ByteBuffer buf = BufferPoolUtils.allocate(TioConfig.WRITE_CHUNK_SIZE, 64 * 1024);
        try {
          int readBytes;
          while ((readBytes = fc.read(buf)) != -1) {
            if (readBytes == 0) {
              continue;
            }
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
            // 同步写出（这里之后也可以用类似退避逻辑再优化）
            while (encrypted.hasRemaining()) {
              sc.write(encrypted);
            }
            buf.clear();
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
              Tio.close(channelContext, "An exception occurred during SSL encryption.", ChannelCloseCode.SSL_ENCRYPTION_ERROR);
            }
          }
        }
        sendByteBuffer(byteBuffer, nextPacket);
      } else {
        channelContext.isSending.set(false);
      }
    }
  }
}