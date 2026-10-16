package nexus.io.tio.core.task;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.aio.Packet;
import nexus.io.tio.core.ChannelCloseCode;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.TioConfig;
import nexus.io.tio.core.exception.AioDecodeException;
import nexus.io.tio.core.exception.TioDecodeException;
import nexus.io.tio.core.stat.ChannelStat;
import nexus.io.tio.core.stat.IpStat;
import nexus.io.enhance.buffer.VirtualBuffer;
import nexus.io.tio.core.pool.BufferPoolUtils;
import nexus.io.tio.exception.TioHandlePacketException;
import nexus.io.tio.utils.SystemTimer;
import nexus.io.tio.utils.hutool.CollUtil;

@SuppressWarnings("deprecation")
public class DecodeTask {
  private static final Logger log = LoggerFactory.getLogger(DecodeTask.class);
  private final static boolean DIAGNOSTIC_LOG_ENABLED = TioConfig.disgnostic;

  /**
   * 上一次解码剩下的数据
   */
  private ByteBuffer lastByteBuffer = null;
  private VirtualBuffer accumulation;
  private boolean decoding;
  private volatile boolean closed;
  private final ReentrantLock decodeLock = new ReentrantLock();

  /** Returned packets must own their payload; decoder input is valid only during decode. */
  public void close() {
    closed = true;
    // Never wait for a business handler while closing its connection. If decoding is
    // active, its finally block owns the release (also safe for reentrant close).
    if (decodeLock.tryLock()) {
      try {
        if (!decoding) releaseAccumulation();
      } finally {
        decodeLock.unlock();
      }
    }
  }

  private void releaseAccumulation() {
    lastByteBuffer = null;
    VirtualBuffer owned = accumulation;
    accumulation = null;
    if (owned != null) owned.clean();
  }

  private ByteBuffer append(ByteBuffer input) {
    int retained = lastByteBuffer.remaining();
    long required = (long) retained + input.remaining();
    if (required > Integer.MAX_VALUE) throw new IllegalArgumentException("Decode buffer overflow");
    ByteBuffer target = accumulation.buffer();
    if (required > target.capacity()) {
      int capacity = (int) Math.min(Integer.MAX_VALUE, Math.max(required, (long) target.capacity() * 2));
      VirtualBuffer replacement = BufferPoolUtils.allocateRequest(capacity);
      replacement.buffer().put(lastByteBuffer);
      accumulation.clean();
      accumulation = replacement;
      target = replacement.buffer();
    } else {
      // Compact only after a complete packet consumed a prefix, never on every read.
      if (lastByteBuffer.position() != 0) target.compact();
      else target.position(target.limit()).limit(target.capacity());
    }
    target.put(input).flip();
    lastByteBuffer = null;
    return target;
  }

  private void retain(ByteBuffer source, int start, int limit) {
    source.limit(limit).position(start);
    if (accumulation == null) {
      accumulation = BufferPoolUtils.allocateRequest(Math.max(1024, source.remaining()));
      accumulation.buffer().put(source).flip();
    } else if (start > 0 && source.capacity() > 65536 && source.remaining() < source.capacity() / 4) {
      // A large packet followed by a tiny partial packet must not pin its large buffer.
      VirtualBuffer replacement = BufferPoolUtils.allocateRequest(Math.max(1024, source.remaining()));
      replacement.buffer().put(source).flip();
      accumulation.clean();
      accumulation = replacement;
    }
    lastByteBuffer = accumulation.buffer();
  }


  /**
   * 上次解码进度百分比
   */
  private int lastPercentage = 0;

  public void decode(ChannelContext channelContext, ByteBuffer byteBuffer) {
    decodeLock.lock();
    try {
      if (closed) return;
      decoding = true;
      try {
        decodeAvailable(channelContext, byteBuffer);
      } catch (Throwable error) {
        releaseAccumulation();
        throw error;
      } finally {
        decoding = false;
        if (closed || lastByteBuffer == null) releaseAccumulation();
      }
    } finally {
      decodeLock.unlock();
      // Covers close racing between the final closed check and unlocking.
      if (closed) close();
    }
  }

  private void decodeAvailable(ChannelContext channelContext, ByteBuffer byteBuffer) {
    TioConfig tioConfig = channelContext.tioConfig;
    if (DIAGNOSTIC_LOG_ENABLED) {
      log.info("decode:{}", channelContext.getClientNode());
    }
    if (lastByteBuffer != null) {
      byteBuffer = append(byteBuffer);
    }
    label_2: while (!closed) {
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
            retain(byteBuffer, initPosition, limit);
            return;
          }
        } else {
          packet = tioConfig.getAioHandler().decode(byteBuffer, limit, initPosition, readableLength, channelContext);
        }

        if (packet == null) {
          // 数据不够，解不了码
          retain(byteBuffer, initPosition, limit);
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
            channelContext.executeOrdered(biz, new Runnable() {
              @Override
              public void run() {
                try {
                  new HandlePacketTask().handle(channelContext, p);
                } catch (Throwable e) {
                  log.error("HandlePacketTask error, {}", channelContext, e);
                  Tio.close(channelContext, e, "Business handler failed", ChannelCloseCode.DECODE_ERROR);
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

        lastByteBuffer = null;
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
