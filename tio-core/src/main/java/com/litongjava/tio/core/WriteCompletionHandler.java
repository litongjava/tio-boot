package com.litongjava.tio.core;

import java.nio.channels.CompletionHandler;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.aio.PacketMeta;
import com.litongjava.tio.consts.TioCoreConfigKeys;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.task.SendPacketTask;
import com.litongjava.tio.core.vo.WriteCompletionVo;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.CollUtil;

/**
 * @author tanyaowu
 */
public class WriteCompletionHandler implements CompletionHandler<Integer, WriteCompletionVo> {
  private static final Logger log = LoggerFactory.getLogger(WriteCompletionHandler.class);
  private static final boolean DIAGNOSTIC_LOG_ENABLED = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_DIAGNOSTIC,
      false);

  private final ChannelContext channelContext;

  public WriteCompletionHandler(ChannelContext channelContext) {
    this.channelContext = channelContext;
  }

  @Override
  public void completed(Integer bytesWritten, WriteCompletionVo writeCompletionVo) {
    if (DIAGNOSTIC_LOG_ENABLED) {
      log.info("write:{},{}", channelContext.getClientNode(), bytesWritten);
    }

    if (bytesWritten == null) {
      // Treat null as failure and delegate to failed()
      failed(new NullPointerException("bytesWritten is null"), writeCompletionVo);
      return;
    }

    // Windows AsynchronousSocketChannel may complete a write with 0 bytes written.
    // This is not necessarily a fatal error. We should retry writing remaining
    // bytes.
    if (bytesWritten == 0) {
      if (writeCompletionVo.getByteBuffer() != null && writeCompletionVo.getByteBuffer().hasRemaining()) {
        // Re-issue write for the remaining data instead of closing the channel.
        channelContext.asynchronousSocketChannel.write(writeCompletionVo.getByteBuffer(), writeCompletionVo, this);
        return;
      }
      // If buffer has no remaining and bytesWritten is 0, fall through to handle() as
      // a no-op/stat update.
      // We do NOT close for 0 in this path.
    }

    if (bytesWritten > 0) {
      writeCompletionVo.setTotalWritten(writeCompletionVo.getTotalWritten() + bytesWritten);
      channelContext.stat.latestTimeOfSentByte = SystemTimer.currTime;
    }

    if (writeCompletionVo.getByteBuffer() != null && writeCompletionVo.getByteBuffer().hasRemaining()) {
      channelContext.asynchronousSocketChannel.write(writeCompletionVo.getByteBuffer(), writeCompletionVo, this);
    } else {
      handle(writeCompletionVo.getTotalWritten(), null, writeCompletionVo);
      BufferPoolUtils.clean(writeCompletionVo.getByteBuffer());
      processNextPacket(channelContext);
    }
  }

  @Override
  public void failed(Throwable throwable, WriteCompletionVo writeCompletionVo) {
    // When write fails, always report throwable to handle() so we can close with
    // real cause.
    handle(0, throwable, writeCompletionVo);
    BufferPoolUtils.clean(writeCompletionVo.getByteBuffer());
    processNextPacket(channelContext);
  }

  private void processNextPacket(ChannelContext channelContext) {
    channelContext.isSending.set(false);
    new SendPacketTask(channelContext).processSendQueue();
  }

  /**
   * @param bytesWritten      total bytes written for this send operation
   * @param throwable         write failure cause (if any)
   * @param writeCompletionVo attachment
   */
  public void handle(Integer bytesWritten, Throwable throwable, WriteCompletionVo writeCompletionVo) {
    channelContext.stat.latestTimeOfSentPacket = SystemTimer.currTime;

    Object attachment = writeCompletionVo.getObj();
    TioConfig tioConfig = channelContext.tioConfig;

    boolean isSentSuccess = (bytesWritten != null && bytesWritten > 0);

    if (isSentSuccess) {
      if (tioConfig.statOn) {
        tioConfig.groupStat.sentBytes.addAndGet(bytesWritten);
        channelContext.stat.sentBytes.addAndGet(bytesWritten);
      }

      if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
        for (Long v : tioConfig.ipStats.durationList) {
          IpStat ipStat = (IpStat) channelContext.tioConfig.ipStats.get(v, channelContext);
          ipStat.getSentBytes().addAndGet(bytesWritten);
        }
      }
    }

    try {
      boolean isPacket = attachment instanceof Packet;

      if (isPacket) {
        if (isSentSuccess) {
          if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
            for (Long v : tioConfig.ipStats.durationList) {
              IpStat ipStat = (IpStat) channelContext.tioConfig.ipStats.get(v, channelContext);
              ipStat.getSentPackets().incrementAndGet();
            }
          }
        }
        handleOne(bytesWritten, throwable, (Packet) attachment, isSentSuccess);
      } else {
        List<?> ps = (List<?>) attachment;
        for (Object obj : ps) {
          handleOne(bytesWritten, throwable, (Packet) obj, isSentSuccess);
        }
      }

      if (!isSentSuccess) {
//        if (throwable != null) {
//          Tio.close(channelContext, throwable,
//              "Write failed: bytesWritten=" + bytesWritten + ", cause=" + throwable.getMessage(),
//              ChannelCloseCode.WRITE_COUNT_IS_NEGATIVE);
//        } else if (bytesWritten != null && bytesWritten < 0) {
//          Tio.close(channelContext, null, "Write invalid: bytesWritten=" + bytesWritten,
//              ChannelCloseCode.WRITE_COUNT_IS_NEGATIVE);
//        }
        
        Tio.close(channelContext, throwable, "Write data return:" + bytesWritten, ChannelCloseCode.WRITE_COUNT_IS_NEGATIVE);
      }
    } catch (Throwable e) {
      log.error(e.toString(), e);
    }
  }

  public void handleOne(Integer result, Throwable throwable, Packet packet, Boolean isSentSuccess) {
    PacketMeta meta = packet.getMeta();
    if (meta != null) {
      meta.setIsSentSuccess(isSentSuccess);
      if (meta.getCountDownLatch() != null) {
        meta.getCountDownLatch().countDown();
      }
    }
    try {
      channelContext.processAfterSent(packet, isSentSuccess);
    } catch (Throwable e) {
      log.error(e.toString(), e);
    }

    if (!packet.isKeepConnection()) {
      String msg = "remove conneciton because KeepedConnection is false:" + packet.logstr();
      if (DIAGNOSTIC_LOG_ENABLED) {
        log.info(msg);
      }
      Tio.close(channelContext, msg);
    }
  }
}