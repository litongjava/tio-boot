package nexus.io.tio.core.udp.task;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.core.udp.UdpPacket;
import nexus.io.tio.core.udp.intf.UdpHandler;

/**
 * @author tanyaowu
 * 2017年7月6日 上午9:47:24
 */
public class UdpHandlerRunnable implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(UdpHandlerRunnable.class);

  private final UdpHandler udpHandler;
  private final LinkedBlockingQueue<UdpPacket> queue;
  private final DatagramSocket datagramSocket;
  private final ExecutorService handlerExecutorService;

  private volatile boolean stopped = false;

  public UdpHandlerRunnable(UdpHandler udpHandler, LinkedBlockingQueue<UdpPacket> queue, DatagramSocket datagramSocket,
      ExecutorService handlerExecutorService) {
    this.udpHandler = udpHandler;
    this.queue = queue;
    this.datagramSocket = datagramSocket;
    this.handlerExecutorService = handlerExecutorService;
  }

  @Override
  public void run() {
    while (!stopped) {
      try {
        UdpPacket udpPacket = queue.take();
        dispatch(udpPacket);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if (!stopped) {
          log.error("udp handler thread interrupted", e);
        }
        break;
      } catch (Throwable e) {
        log.error("udp handler loop error", e);
      }
    }
  }

  private void dispatch(UdpPacket udpPacket) {
    if (handlerExecutorService != null) {
      handlerExecutorService.execute(() -> safeHandle(udpPacket));
    } else {
      safeHandle(udpPacket);
    }
  }

  private void safeHandle(UdpPacket udpPacket) {
    try {
      udpHandler.handler(udpPacket, datagramSocket);
    } catch (Throwable e) {
      log.error("udp handler failed, packet={}", udpPacket, e);
    }
  }

  public void stop() {
    stopped = true;
  }
}