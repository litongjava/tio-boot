package com.litongjava.tio.core.task;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelCloseCode;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.AioDecodeException;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.core.stat.ChannelStat;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.utils.ByteBufferUtils;
import com.litongjava.tio.exception.TioHandlePacketException;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.hutool.CollUtil;

@SuppressWarnings("deprecation")
public class DecodeTask {
  private static final Logger log = LoggerFactory.getLogger(DecodeTask.class);
  private final static boolean DIAGNOSTIC_LOG_ENABLED = TioConfig.disgnostic;

  /**
   * 上一次解码剩下的数据
   */
  private ByteBuffer lastByteBuffer = null;

  /**
   * 上次解码进度百分比
   */
  private int lastPercentage = 0;

  public void decode(ChannelContext channelContext, ByteBuffer byteBuffer) {
    TioConfig tioConfig = channelContext.tioConfig;
    if (DIAGNOSTIC_LOG_ENABLED) {
      log.info("decode:{}", channelContext.getClientNode());
    }
    if (lastByteBuffer != null) {
      byteBuffer = ByteBufferUtils.composite(lastByteBuffer, byteBuffer);
      lastByteBuffer = null;
    }
    label_2: while (true) {
      int initPosition = byteBuffer.position();
      int limit = byteBuffer.limit();
      int readableLength = limit - initPosition;
      Packet packet = null;
      try {
        if (channelContext.packetNeededLength != null) {
          if (log.isDebugEnabled()) {
            log.debug("{}, Length required for decoding:{}", channelContext, channelContext.packetNeededLength);
          }
          if (readableLength >= channelContext.packetNeededLength) {
            packet = tioConfig.getAioHandler().decode(byteBuffer, limit, initPosition, readableLength, channelContext);
          } else {
            int percentage = (int) (((double) readableLength / channelContext.packetNeededLength) * 100);
            if (percentage != lastPercentage) {
              lastPercentage = percentage;
              if (DIAGNOSTIC_LOG_ENABLED) {
                log.info("Receiving large packet: received {}% of {} bytes.", percentage,
                    channelContext.packetNeededLength);
              }
            }
            lastByteBuffer = ByteBufferUtils.copy(byteBuffer, initPosition, limit);
            return;
          }
        } else {
          try {
            packet = tioConfig.getAioHandler().decode(byteBuffer, limit, initPosition, readableLength, channelContext);
          } catch (Exception e) {
            log.error("Failed to decode:{}", channelContext, e);
          }
        }

        if (packet == null) {
          // 数据不够，解不了码
          lastByteBuffer = ByteBufferUtils.copy(byteBuffer, initPosition, limit);
          ChannelStat channelStat = channelContext.stat;
          channelStat.decodeFailCount++;
          // 检查慢包攻击
          if (channelContext.getTioConfig().checkAttacks && channelContext.isServer()) {
            if (channelStat.decodeFailCount > 10) {
              // int capacity = lastByteBuffer.capacity();
              int per = readableLength / channelStat.decodeFailCount;
              if (per < Math.min(channelContext.getReadBufferSize() / 2, 256)) {
                String str = "Failed to decode continuously " + channelStat.decodeFailCount
                    + " times unsuccessfully, and the average data received each time is " + per
                    + " bytes, which suggests the possibility of a slow attack";
                log.warn(str);
                throw new AioDecodeException(str);
              }
            }
          }

          if (log.isInfoEnabled() && !channelContext.getTioConfig().ignoreDecodeFail) {
            if (channelStat.decodeFailCount > 3 && channelContext.packetNeededLength == null) {
              if (channelContext.isServer()) {
                log.info(
                    "{} Failed to decode for {} consecutive times, the length of data involved in decoding is {} bytes.",
                    channelContext, channelStat.decodeFailCount, readableLength);
              } else if (log.isDebugEnabled()) {
                log.info(
                    "{} Failed to decode for {} consecutive times, the length of data involved in decoding is {} bytes.",
                    channelContext, channelStat.decodeFailCount, readableLength);
              }

            }
          }

          return;
        } else {
          // 解码成功
          channelContext.setPacketNeededLength(null);
          channelContext.stat.latestTimeOfReceivedPacket = SystemTimer.currTime;
          channelContext.stat.decodeFailCount = 0;

          int packetSize = byteBuffer.position() - initPosition;
          packet.setByteCount(packetSize);

          if (tioConfig.statOn) {
            tioConfig.groupStat.receivedPackets.incrementAndGet();
            channelContext.stat.receivedPackets.incrementAndGet();
          }

          if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
            try {
              for (Long v : tioConfig.ipStats.durationList) {
                IpStat ipStat = tioConfig.ipStats.get(v, channelContext);
                ipStat.getReceivedPackets().incrementAndGet();
                tioConfig.getIpStatListener().onAfterDecoded(channelContext, packet, packetSize, ipStat);
              }
            } catch (Exception e1) {
              log.error(packet.logstr(), e1);
            }
          }

          if (tioConfig.getAioListener() != null) {
            try {
              tioConfig.getAioListener().onAfterDecoded(channelContext, packet, packetSize);
            } catch (Throwable e) {
              log.error(e.toString(), e);
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("{}, Unpacking to get a packet:{}", channelContext, packet.logstr());
          }

          ExecutorService biz = tioConfig.getBizExecutor();
          if (biz != null) {
            final Packet p = packet;
            biz.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  new HandlePacketTask().handle(channelContext, p);
                } catch (Throwable e) {
                  log.error("HandlePacketTask error, {}", channelContext, e);
                }
              }
            });
          } else {
            try {
              new HandlePacketTask().handle(channelContext, packet);
            } catch (TioHandlePacketException e) {
              byteBuffer.mark();
              String request = StandardCharsets.UTF_8.decode(byteBuffer).toString();
              byteBuffer.reset();
              log.error("Failed to handle request:{},packet:{},byteBuffer:{}", request, packet, byteBuffer, e);
            }
          }
          if (byteBuffer.hasRemaining()) {
            // 组包后，还剩有数据
            if (log.isDebugEnabled()) {
              log.debug("{},After grouping packets, there is still data left:{}", channelContext,
                  byteBuffer.remaining());
            }
            continue label_2;
          } else {
            // 组包后，数据刚好用完
            lastByteBuffer = null;
            if (log.isDebugEnabled()) {
              log.debug("{},After grouping the packets, the data just ran out", channelContext);
            }
            return;
          }
        }
      } catch (Throwable e) {
        if (channelContext.logWhenDecodeError) {
          log.error("Encountered an exception while decoding", e);
        }

        channelContext.setPacketNeededLength(null);

        if (e instanceof AioDecodeException || e instanceof TioDecodeException) {
          List<Long> list = tioConfig.ipStats.durationList;
          if (list != null && list.size() > 0) {
            try {
              for (Long v : list) {
                IpStat ipStat = tioConfig.ipStats.get(v, channelContext);
                ipStat.getDecodeErrorCount().incrementAndGet();
                tioConfig.getIpStatListener().onDecodeError(channelContext, ipStat);
              }
            } catch (Exception e1) {
              log.error(e1.toString(), e1);
            }
          }
        } else {
          log.error(e.getMessage(), e);
        }
        Tio.close(channelContext, e, "Decode exception:" + e.getMessage(), ChannelCloseCode.DECODE_ERROR);
        return;
      }
    }
  }
}
