package com.litongjava.tio.core.ssl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.tio.consts.TioConst;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.ssl.facade.IHandshakeCompletedListener;
import com.litongjava.tio.core.task.AfterSslHandshakeCompleted;
import com.litongjava.tio.core.task.SendPacketTask;

/**
 * SSL handshake completion callback.
 *
 * This listener:
 * 1) Marks the SSL handshake as completed.
 * 2) Notifies a latch (if present) so application logic can safely send data AFTER TLS is ready.
 * 3) Flushes any queued packets that were produced before handshake completion.
 */
public class SslHandshakeCompletedListener implements IHandshakeCompletedListener {
  private static final Logger log = LoggerFactory.getLogger(SslHandshakeCompletedListener.class);

  private final ChannelContext channelContext;

  public SslHandshakeCompletedListener(ChannelContext channelContext) {
    this.channelContext = channelContext;
  }

  @Override
  public void onComplete() {
    log.info("{}, Complete SSL handshake", channelContext);

    // Mark handshake completed in the SSL context
    channelContext.sslFacadeContext.setHandshakeCompleted(true);

    // Notify the latch (if application code is waiting for TLS readiness)
    try {
      Object o = channelContext.getAttribute(TioConst.ATTR_SSL_HANDSHAKE_LATCH);
      if (o instanceof CountDownLatch) {
        ((CountDownLatch) o).countDown();
      }
    } catch (Throwable t) {
      // Never fail the connection due to latch notification issues
      log.warn("{}, Failed to count down SSL handshake latch: {}", channelContext, t.toString());
    }

    // Fire "onAfterConnected" for client side when TLS is ready
    if (channelContext.tioConfig.getAioListener() != null) {
      try {
        channelContext.tioConfig.getAioListener().onAfterConnected(channelContext, true, channelContext.isReconnect);
      } catch (Exception e) {
        log.error(e.toString(), e);
      }
    }

    // Flush any business packets that were queued before handshake completion
    ConcurrentLinkedQueue<Packet> pending =
        new AfterSslHandshakeCompleted().getForSendAfterSslHandshakeCompleted(false);

    if (pending == null || pending.isEmpty()) {
      return;
    }

    log.info("{}, There are {} pending packets before SSL handshake", channelContext, pending.size());

    while (true) {
      Packet packet = pending.poll();
      if (packet != null) {
        new SendPacketTask(channelContext).sendPacket(packet);
      } else {
        break;
      }
    }
  }
}
