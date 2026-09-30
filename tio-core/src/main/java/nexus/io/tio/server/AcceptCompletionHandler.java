package nexus.io.tio.server;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.enhance.buffer.VirtualBuffer;
import nexus.io.tio.consts.TioCoreConfigKeys;
import nexus.io.tio.core.ReadCompletionHandler;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.ChannelCloseCode;
import nexus.io.tio.core.pool.BufferPoolUtils;
import nexus.io.tio.core.ssl.SslUtils;
import nexus.io.tio.core.stat.IpStat;
import nexus.io.tio.core.utils.IpBlacklistUtils;
import nexus.io.tio.utils.SystemTimer;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.hutool.CollUtil;

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
    ServerChannelContext channelContext = null;
    VirtualBuffer attachment = null;
    boolean handedOff = false;
    try {
      AsynchronousServerSocketChannel server = tioServer.getServerSocketChannel();
      if (tioServer.isWaitingStop() || server == null || !server.isOpen()) {
        return;
      }
      // A failure to rearm accept must not discard the connection already accepted.
      rearm(tioServer);
      String clientIp = null;
      int port = 0;
      InetSocketAddress inetSocketAddress = (InetSocketAddress) clientSocketChannel.getRemoteAddress();
      clientIp = inetSocketAddress.getHostString();
      port = inetSocketAddress.getPort();
      if (DIAGNOSTIC_LOG_ENABLED) {
        log.info("new connection:{},{}", clientIp, port);
      }

      ServerTioConfig serverTioConfig = tioServer.getServerTioConfig();

      if (IpBlacklistUtils.isInBlacklist(serverTioConfig, clientIp)) {
        log.info("{} on the blacklist, {}", clientIp, serverTioConfig.getName());
        return;
      }

      if (serverTioConfig.statOn) {
        ((ServerGroupStat) serverTioConfig.groupStat).accepted.incrementAndGet();
      }

      clientSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      clientSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
      clientSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
      clientSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

      channelContext = new ServerChannelContext(serverTioConfig, clientSocketChannel,
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
        attachment = BufferPoolUtils.allocateRequest(channelContext.getReadBufferSize());
        ByteBuffer readByteBuffer = attachment.buffer();
        readByteBuffer.position(0);
        readByteBuffer.limit(readByteBuffer.capacity());
        clientSocketChannel.read(readByteBuffer, attachment, readCompletionHandler);
        attachment = null;
        handedOff = true;
      }

    } catch (Throwable error) {
      log.error("Failed to initialize the accepted connection", error);
    } finally {
      if (!handedOff) {
        if (attachment != null) {
          try {
            attachment.clean();
          } catch (Throwable error) {
            log.error("Failed to release the initial read buffer", error);
          }
        }
        try {
          if (channelContext != null) {
            Tio.close(channelContext, "connection initialization failed", ChannelCloseCode.READ_ERROR);
          }
        } catch (Throwable error) {
          log.error("Failed to clean up the accepted connection context", error);
        } finally {
          try {
            clientSocketChannel.close();
          } catch (Throwable error) {
            log.error("Failed to close the accepted socket", error);
          }
        }
      }
    }
  }

  private void rearm(TioServer tioServer) {
    try {
      AsynchronousServerSocketChannel server = tioServer.getServerSocketChannel();
      if (!tioServer.isWaitingStop() && server != null && server.isOpen()) {
        server.accept(tioServer, this);
      }
    } catch (Throwable error) {
      log.error("Failed to rearm accept; check the listening channel state", error);
    }
  }

  @Override
  public void failed(Throwable exc, TioServer tioServer) {
    log.error("Failed to accept a connection", exc);
    rearm(tioServer);
  }
}
