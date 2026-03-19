package com.litongjava.tio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.enhance.buffer.VirtualBuffer;
import com.litongjava.tio.consts.TioCoreConfigKeys;
import com.litongjava.tio.core.ReadCompletionHandler;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.core.ssl.SslUtils;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.utils.IpBlacklistUtils;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.CollUtil;

/**
 * @author tanyaowu 2017年4月4日 上午9:27:45
 */
public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, TioServer> {
  private static final Logger log = LoggerFactory.getLogger(AcceptCompletionHandler.class);
  private final static boolean DIAGNOSTIC_LOG_ENABLED = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_DIAGNOSTIC, false);

  /**
   *
   * @param clientSocketChannel
   * @param tioServer
   */
  @Override
  public void completed(AsynchronousSocketChannel clientSocketChannel, TioServer tioServer) {
    AsynchronousServerSocketChannel serverSocketChannel = tioServer.getServerSocketChannel();

    if (tioServer.isWaitingStop()) {
      log.info("The server will be shut down and no new requests will be accepted:{}", tioServer.getServerNode());
    } else {
      serverSocketChannel.accept(tioServer, this);
    }
    if (serverSocketChannel == null) {
      log.info("receive serverSocketChannel is null skip");
      return;
    }

    if (!serverSocketChannel.isOpen()) {
      log.info("receive serverSocketChannel is not open skip");
      return;
    }

    String clientIp = null;
    int port = 0;
    InetSocketAddress inetSocketAddress;
    try {
      inetSocketAddress = (InetSocketAddress) clientSocketChannel.getRemoteAddress();
      clientIp = inetSocketAddress.getHostString();
      port = inetSocketAddress.getPort();
      if (DIAGNOSTIC_LOG_ENABLED) {
        log.info("new connection:{},{}", clientIp, port);
      }
    } catch (IOException e1) {
      log.error("Failed to get client ip and port", e1);
      try {
        clientSocketChannel.close();
      } catch (Exception closeEx) {
        log.error("Failed to close socket after exception", closeEx);
      }
      return;
    }

    ServerTioConfig serverTioConfig = tioServer.getServerTioConfig();

    try {
      if (IpBlacklistUtils.isInBlacklist(serverTioConfig, clientIp)) {
        log.info("{} on the blacklist, {}", clientIp, serverTioConfig.getName());
        clientSocketChannel.close();
        return;
      }

      if (serverTioConfig.statOn) {
        ((ServerGroupStat) serverTioConfig.groupStat).accepted.incrementAndGet();
      }

      clientSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      clientSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
      clientSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
      clientSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

      ServerChannelContext channelContext = new ServerChannelContext(serverTioConfig, clientSocketChannel,
          //
          clientIp, port);

      channelContext.setClosed(false);
      channelContext.stat.setTimeFirstConnected(SystemTimer.currTime);
      channelContext.setServerNode(tioServer.getServerNode());
      boolean isConnected = true;
      boolean isReconnect = false;
      if (serverTioConfig.getServerAioListener() != null) {
        if (!SslUtils.isSsl(channelContext.tioConfig)) {
          try {
            serverTioConfig.getServerAioListener().onAfterConnected(channelContext, isConnected, isReconnect);
          } catch (Throwable e) {
            log.error("ServerAioListener onAfterConnected:", e);
          }
        }
      }

      if (CollUtil.isNotEmpty(serverTioConfig.ipStats.durationList)) {
        try {
          for (Long v : serverTioConfig.ipStats.durationList) {
            IpStat ipStat = (IpStat) serverTioConfig.ipStats.get(v, channelContext);
            ipStat.getRequestCount().incrementAndGet();
            serverTioConfig.getIpStatListener().onAfterConnected(channelContext, isConnected, isReconnect, ipStat);
          }
        } catch (Exception e) {
          log.error("IpStatListener onAfterConnected:", e);
        }
      }

      if (!tioServer.isWaitingStop()) {
        ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler(channelContext);
        VirtualBuffer attachment = BufferPoolUtils.allocateRequest(channelContext.getReadBufferSize());
        ByteBuffer readByteBuffer = attachment.buffer();
        readByteBuffer.position(0);
        readByteBuffer.limit(readByteBuffer.capacity());
        clientSocketChannel.read(readByteBuffer, attachment, readCompletionHandler);
      }
    } catch (Throwable e) {
      log.error("Failed to read data from :{},{}", clientIp, port, e);
    }
  }

  /**
   *
   * @param exc
   * @param tioServer
   */
  @Override
  public void failed(Throwable exc, TioServer tioServer) {
    if (tioServer.isWaitingStop()) {
      log.info("The server will be shut down and no new requests will be accepted:{}", tioServer.getServerNode());
    } else {
      AsynchronousServerSocketChannel serverSocketChannel = tioServer.getServerSocketChannel();
      serverSocketChannel.accept(tioServer, this);
      log.error("[" + tioServer.getServerNode() + "] listening exception", exc);
    }

  }
}
