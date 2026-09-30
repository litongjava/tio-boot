package nexus.io.tio.core;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.enhance.buffer.VirtualBuffer;
import nexus.io.tio.core.pool.BufferPoolUtils;
import nexus.io.tio.core.stat.IpStat;
import nexus.io.tio.core.task.DecodeTask;
import nexus.io.tio.core.utils.ByteBufferUtils;
import nexus.io.tio.core.utils.TioUtils;
import nexus.io.tio.utils.SystemTimer;
import nexus.io.tio.utils.hutool.CollUtil;

/**
 *
 * @author tanyaowu 2017年4月4日 上午9:22:04
 */
public class ReadCompletionHandler implements CompletionHandler<Integer, VirtualBuffer> {
  private static Logger log = LoggerFactory.getLogger(ReadCompletionHandler.class);
  private ChannelContext channelContext = null;
  private DecodeTask decodeTask;

  public ReadCompletionHandler(ChannelContext channelContext) {
    this.channelContext = channelContext;
    this.decodeTask = new DecodeTask();
  }

  /**
   * Decode decrypted SSL data using the same decoder for partial packets.
   */
  public void handlePlainFromSsl(ByteBuffer plainBuffer) {
    if (plainBuffer == null) {
      return;
    }
    try {
      // plainBuffer must be readable between position and limit.
      decodeTask.decode(channelContext, plainBuffer);
    } catch (Throwable e) {
      log.error("Decode error (plain from ssl)", e);
      closeSafely(e, "unexpected decode error (plain from ssl)", ChannelCloseCode.DECODE_ERROR);
    }
  }

  @Override
  public void completed(Integer result, VirtualBuffer virtualBuffer) {
    // This callback owns the buffer until the next read is submitted successfully.
    VirtualBuffer current = virtualBuffer;
    boolean submitted = false;
    try {
      ByteBuffer byteBuffer = current.buffer();
      if (result > 0) {
        TioConfig tioConfig = channelContext.tioConfig;
        if (tioConfig.statOn) {
          tioConfig.groupStat.receivedBytes.addAndGet(result);
          tioConfig.groupStat.receivedTcps.incrementAndGet();
          channelContext.stat.receivedBytes.addAndGet(result);
          channelContext.stat.receivedTcps.incrementAndGet();
        }

        channelContext.stat.latestTimeOfReceivedByte = SystemTimer.currTime;

        if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
          try {
            for (Long v : tioConfig.ipStats.durationList) {
              IpStat ipStat = tioConfig.ipStats.get(v, channelContext);
              ipStat.getReceivedBytes().addAndGet(result);
              ipStat.getReceivedTcps().incrementAndGet();
              tioConfig.getIpStatListener().onAfterReceivedBytes(channelContext, result, ipStat);
            }
          } catch (Exception e1) {
            log.error(channelContext.toString(), e1);
          }
        }

        if (tioConfig.getAioListener() != null) {
          try {
            tioConfig.getAioListener().onAfterReceivedBytes(channelContext, result);
          } catch (Exception e) {
            log.error(channelContext.toString(), e);
          }
        }

        byteBuffer.flip();
        if (channelContext.sslFacadeContext == null) {
          try {
            decodeTask.decode(channelContext, byteBuffer);
          } catch (Throwable error) {
            log.error("Failed to decode received data", error);
            closeSafely(error, "unexpected decode error", ChannelCloseCode.DECODE_ERROR);
            return;
          }
        } else {
          try {
            channelContext.sslFacadeContext.getSslFacade().decrypt(ByteBufferUtils.copy(byteBuffer));
          } catch (Throwable error) {
            log.error("Failed to decrypt received data", error);
            closeSafely(error, "SSL decrypt error", ChannelCloseCode.SSL_DECRYPT_ERROR);
            return;
          }
        }
        if (TioUtils.checkBeforeIO(channelContext)) {
          if (byteBuffer.capacity() != channelContext.getReadBufferSize()) {
            release(current);
            current = null;
            current = BufferPoolUtils.allocateRequest(channelContext.getReadBufferSize());
            byteBuffer = current.buffer();
          }
          byteBuffer.clear();
          channelContext.asynchronousSocketChannel.read(byteBuffer, current, this);
          submitted = true;
        }
      } else {
        ChannelCloseCode code = result == 0 ? ChannelCloseCode.READ_COUNT_IS_ZERO
            : result == -1 ? ChannelCloseCode.CLOSED_BY_PEER : ChannelCloseCode.READ_COUNT_IS_NEGATIVE;
        closeSafely(null, "read completed with result " + result, code);
      }
    } catch (Throwable error) {
      log.error("Unhandled read completion error", error);
      closeSafely(error, "unexpected error in read completion", ChannelCloseCode.READ_ERROR);
    } finally {
      if (!submitted) {
        release(current);
      }
    }
  }

  private static void release(VirtualBuffer buffer) {
    if (buffer != null) {
      try {
        buffer.clean();
      } catch (Throwable error) {
        log.error("Failed to release the read buffer", error);
      }
    }
  }

  private void closeSafely(Throwable error, String remark, ChannelCloseCode code) {
    try {
      Tio.close(channelContext, error, remark, code);
    } catch (Throwable closeError) {
      log.error("Failed to clean up the connection after a read error", closeError);
      try {
        if (channelContext.asynchronousSocketChannel != null) {
          channelContext.asynchronousSocketChannel.close();
        }
      } catch (Throwable socketError) {
        log.error("Failed to close the socket", socketError);
      }
    }
  }

  @Override
  public void failed(Throwable exc, VirtualBuffer virtualBuffer) {
    try {
      closeSafely(exc, "Failed to read data", ChannelCloseCode.READ_ERROR);
    } finally {
      release(virtualBuffer);
    }
  }

  public DecodeTask getDecodeTask() {
    return decodeTask;
  }

}
