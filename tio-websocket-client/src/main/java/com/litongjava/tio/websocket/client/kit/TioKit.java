package com.litongjava.tio.websocket.client.kit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.aio.PacketMeta;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.PacketSendMode;
import com.litongjava.tio.core.task.SendPacketTask;

public class TioKit {
  private static Logger log = LoggerFactory.getLogger(TioKit.class);

  public static Boolean bSend(ChannelContext channelContext, Packet packet, int timeout, TimeUnit timeUnit) {
    if (channelContext == null) {
      return false;
    }
    CountDownLatch countDownLatch = new CountDownLatch(1);
    PacketSendMode packetSendMode = PacketSendMode.SINGLE_BLOCK;
    try {
      if (packet == null || channelContext == null) {
        if (countDownLatch != null) {
          countDownLatch.countDown();
        }
        return false;
      }

      if (channelContext.isVirtual) {
        if (countDownLatch != null) {
          countDownLatch.countDown();
        }
        return true;
      }

      if (channelContext.isClosed || channelContext.isRemoved) {
        if (countDownLatch != null) {
          countDownLatch.countDown();
        }
        if (channelContext != null) {
          log.info("can't send data, {}, isClosed:{}, isRemoved:{}", channelContext, channelContext.isClosed,
              channelContext.isRemoved);
        }
        return false;
      }

      boolean isSingleBlock = countDownLatch != null && packetSendMode == PacketSendMode.SINGLE_BLOCK;

      if (countDownLatch != null) {
        PacketMeta meta = new PacketMeta();
        meta.setCountDownLatch(countDownLatch);
        packet.setMeta(meta);
      }

      boolean sendInitiated = new SendPacketTask(channelContext).sendPacket(packet);

      if (!sendInitiated) {
        if (countDownLatch != null) {
          countDownLatch.countDown();
        }
        return false;
      }

      if (isSingleBlock) {
        try {
          Boolean awaitFlag = countDownLatch.await(timeout, timeUnit);
          if (!awaitFlag) {
            log.error("{}, sync send timeout, timeout:{}s, packet:{}", channelContext, timeUnit.toSeconds(timeout),
                packet.logstr());
          }
        } catch (InterruptedException e) {
          log.error(e.toString(), e);
        }

        Boolean isSentSuccess = packet.getMeta().getIsSentSuccess();
        return isSentSuccess;
      } else {
        return true;
      }
    } catch (Throwable e) {
      log.error(channelContext + ", " + e.toString(), e);
      return false;
    }
  }
}
