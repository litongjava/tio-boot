package com.litongjava.tio.core;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.enhance.buffer.VirtualBuffer;
import com.litongjava.tio.consts.TioCoreConfigKeys;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.task.DecodeTask;
import com.litongjava.tio.core.utils.ByteBufferUtils;
import com.litongjava.tio.core.utils.TioUtils;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.CollUtil;

/**
 *
 * @author tanyaowu 2017年4月4日 上午9:22:04
 */
public class ReadCompletionHandler implements CompletionHandler<Integer, VirtualBuffer> {
  private final static boolean DIAGNOSTIC_LOG_ENABLED =
      EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_DIAGNOSTIC, false);

  private static Logger log = LoggerFactory.getLogger(ReadCompletionHandler.class);
  private ChannelContext channelContext = null;
  private DecodeTask decodeTask;

  public ReadCompletionHandler(ChannelContext channelContext) {
    this.channelContext = channelContext;
    this.decodeTask = new DecodeTask();
  }

  /**
   * 关键新增：SSL解密后的明文从这里进入，复用同一个 decodeTask 做半包累计
   */
  public void handlePlainFromSsl(ByteBuffer plainBuffer) {
    if (plainBuffer == null) {
      return;
    }
    try {
      // plainBuffer 要处于可读状态（position..limit）
      decodeTask.decode(channelContext, plainBuffer);
    } catch (Throwable e) {
      log.error("Decode error (plain from ssl)", e);
      Tio.close(channelContext, e, "unexpected decode error (plain from ssl)", ChannelCloseCode.DECODE_ERROR);
    }
  }

  @Override
  public void completed(Integer result, VirtualBuffer virtualBuffer) {
    ByteBuffer byteBuffer = virtualBuffer.buffer();
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
        } catch (Throwable e) {
          log.error("Decode error", e);
          virtualBuffer.clean();
          Tio.close(channelContext, e, "unexpected decode error", ChannelCloseCode.DECODE_ERROR);
          return;
        }
      } else {
        ByteBuffer copiedByteBuffer = null;
        try {
          copiedByteBuffer = ByteBufferUtils.copy(byteBuffer);
          log.debug("{},Decrypt SSL data:{}", channelContext, copiedByteBuffer);
          channelContext.sslFacadeContext.getSslFacade().decrypt(copiedByteBuffer);
        } catch (Exception e) {
          log.error(channelContext + ", " + e.toString() + copiedByteBuffer, e);
          Tio.close(channelContext, e, e.toString(), ChannelCloseCode.SSL_DECRYPT_ERROR);
        }
      }

      if (TioUtils.checkBeforeIO(channelContext)) {
        read(byteBuffer, virtualBuffer);
      } else {
        virtualBuffer.clean();
      }

    } else if (result == 0) {
      String message = "The length of the read data is 0";
      log.error("close {}, because {}", channelContext, message);
      try {
        Tio.close(channelContext, null, message, ChannelCloseCode.READ_COUNT_IS_ZERO);
      } finally {
        virtualBuffer.clean();
      }
      return;
    } else if (result < 0) {
      if (result == -1) {
        String message = "The connection closed by peer";
        if (DIAGNOSTIC_LOG_ENABLED) {
          log.info("close {}, because {}", channelContext, message);
        }
        try {
          Tio.close(channelContext, null, message, ChannelCloseCode.CLOSED_BY_PEER);
        } finally {
          virtualBuffer.clean();
        }
        return;
      } else {
        String message = "The length of the read data is less than -1";
        log.error("close {}, because {}", channelContext, message);
        try {
          Tio.close(channelContext, null, "read result" + result, ChannelCloseCode.READ_COUNT_IS_NEGATIVE);
        } catch (Exception e) {
          virtualBuffer.clean();
        }
        return;
      }
    }
  }

  private void read(ByteBuffer readByteBuffer, VirtualBuffer virtualBuffer) {
    if (readByteBuffer.capacity() == channelContext.getReadBufferSize()) {
      readByteBuffer.position(0);
      readByteBuffer.limit(readByteBuffer.capacity());
    } else {
      virtualBuffer.clean();
      virtualBuffer = BufferPoolUtils.allocateRequest(channelContext.getReadBufferSize());
      readByteBuffer = virtualBuffer.buffer();
    }

    channelContext.asynchronousSocketChannel.read(readByteBuffer, virtualBuffer, this);
  }

  @Override
  public void failed(Throwable exc, VirtualBuffer virtualBuffer) {
    if (exc instanceof ClosedChannelException) {
      try {
        virtualBuffer.clean();
      } catch (Exception e) {
      }
    }
    Tio.close(channelContext, exc, "Failed to read data: " + exc.getClass().getName(), ChannelCloseCode.READ_ERROR);
  }

  public DecodeTask getDecodeTask() {
    return decodeTask;
  }
  
}
