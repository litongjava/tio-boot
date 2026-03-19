package com.litongjava.tio.client;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.enhance.buffer.VirtualBuffer;
import com.litongjava.tio.client.intf.ClientAioListener;
import com.litongjava.tio.consts.TioConst;
import com.litongjava.tio.core.ChannelCloseCode;
import com.litongjava.tio.core.Node;
import com.litongjava.tio.core.ReadCompletionHandler;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.core.ssl.SslFacadeContext;
import com.litongjava.tio.core.ssl.SslUtils;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.core.task.DecodeTask;
import com.litongjava.tio.proxy.ProxyHandshake;
import com.litongjava.tio.proxy.ProxyInfo;
import com.litongjava.tio.proxy.ProxyType;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.hutool.CollUtil;

/**
 * Just for Client
 */
public class ConnectionCompletionHandler implements CompletionHandler<Void, ConnectionCompletionVo> {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void completed(Void result, ConnectionCompletionVo attachment) {
    handler(result, attachment, null);
  }

  @Override
  public void failed(Throwable throwable, ConnectionCompletionVo attachment) {
    handler(null, attachment, throwable);
  }

  private void handler(Void result, ConnectionCompletionVo attachment, Throwable throwable) {
    ClientChannelContext channelContext = attachment.getChannelContext();
    AsynchronousSocketChannel asynchronousSocketChannel = attachment.getAsynchronousSocketChannel();
    TioClient tioClient = attachment.getTioClient();
    ClientTioConfig clientTioConfig = tioClient.getClientTioConfig();

    Node serverNode = attachment.getServerNode(); // This is the REAL target node (e.g.
                                                  // generativelanguage.googleapis.com:443)
    String bindIp = attachment.getBindIp();
    Integer bindPort = attachment.getBindPort();

    ClientAioListener clientAioListener = clientTioConfig.getClientAioListener();
    boolean isReconnect = attachment.isReconnect();
    boolean isConnected = false;

    // Prepare TLS peer info early. We MUST use the real target host/port for SNI
    // and (optional) HTTPS endpoint check.
    final String tlsPeerHost = (serverNode != null ? serverNode.getHost() : null);
    final Integer tlsPeerPort = (serverNode != null ? serverNode.getPort() : null);

    try {
      if (throwable == null) {

        // 1) Perform proxy handshake FIRST using the raw socket. Do NOT touch
        // channelContext here (it may be null).
        try {
          ProxyInfo proxyInfo = attachment.getProxyInfo();
          Node targetNode = attachment.getServerNode();

          if (proxyInfo != null && proxyInfo.getProxyType() != ProxyType.NONE) {
            if (targetNode == null) {
              throw new RuntimeException("proxy enabled but targetNode is null");
            }

            String proxyUser = proxyInfo.getProxyUser();
            String proxyPass = proxyInfo.getProxyPass();
            String targetHost = targetNode.getHost();
            int targetPort = targetNode.getPort();

            ProxyType pt = proxyInfo.getProxyType();
            if (pt == ProxyType.HTTP) {
              ProxyHandshake.httpConnect(asynchronousSocketChannel, targetHost, targetPort, proxyUser, proxyPass);
            } else if (pt == ProxyType.SOCKS5) {
              ProxyHandshake.socks5Connect(asynchronousSocketChannel, targetHost, targetPort, proxyUser, proxyPass);
            }
          }
        } catch (Throwable ex) {
          log.error("proxy handshake failed", ex);

          // If channelContext is still null (first connect), create a minimal one so we
          // can close / enqueue reconnect safely.
          if (channelContext == null) {
            try {
              channelContext = new ClientChannelContext(clientTioConfig, asynchronousSocketChannel);
              channelContext.setServerNode(serverNode);
              channelContext.setBindIp(bindIp);
              channelContext.setBindPort(bindPort);
              attachment.setChannelContext(channelContext);
            } catch (Throwable ignore) {
              // If even this fails, we can only close the socket silently.
            }
          }

          boolean queued = false;
          try {
            queued = ReconnConf.put(channelContext);
          } catch (Throwable ignore) {
          }

          if (!queued) {
            try {
              com.litongjava.tio.core.Tio.close(channelContext, null, "proxy handshake failed: " + ex.getMessage(),
                  true, false, ChannelCloseCode.CLIENT_CONNECTION_FAIL);
            } catch (Throwable ignore) {
              // ignore
            }
          }

          CountDownLatch latch = attachment.getCountDownLatch();
          if (latch != null) {
            latch.countDown();
          }
          return;
        }

        // 2) Build/refresh channelContext ONLY after proxy handshake succeeded.
        if (isReconnect) {
          channelContext.setAsynchronousSocketChannel(asynchronousSocketChannel);
          clientTioConfig.closeds.remove(channelContext);

          // Ensure serverNode is present after reconnect.
          if (channelContext.getServerNode() == null && serverNode != null) {
            channelContext.setServerNode(serverNode);
          }
        } else {
          channelContext = new ClientChannelContext(clientTioConfig, asynchronousSocketChannel);
          channelContext.setServerNode(serverNode);
        }

        // 3) CRITICAL: Set TLS peer attributes BEFORE SslFacadeContext/SSLFacade is
        // created (it reads attributes immediately).
        if (tlsPeerHost != null && tlsPeerPort != null && tlsPeerPort > 0) {
          channelContext.setAttribute(TioConst.ATTR_TLS_PEER_HOST, tlsPeerHost);
          channelContext.setAttribute(TioConst.ATTR_TLS_PEER_PORT, tlsPeerPort);
        }

        channelContext.setBindIp(bindIp);
        channelContext.setBindPort(bindPort);

        channelContext.getReconnCount().set(0);
        channelContext.setClosed(false);
        isConnected = true;

        attachment.setChannelContext(channelContext);
        clientTioConfig.connecteds.add(channelContext);

        // 4) Start async read loop.
        ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler(channelContext);
        VirtualBuffer vBuffer = BufferPoolUtils.allocateRequest(channelContext.getReadBufferSize());
        ByteBuffer readByteBuffer = vBuffer.buffer();
        readByteBuffer.position(0);
        readByteBuffer.limit(readByteBuffer.capacity());
        asynchronousSocketChannel.read(readByteBuffer, vBuffer, readCompletionHandler);

        log.info("connected to {}", serverNode);

        if (isConnected && !isReconnect) {
          channelContext.stat.setTimeFirstConnected(SystemTimer.currTime);
        }

      } else {
        // Connection establishment failed at TCP level (or similar).
        log.error(throwable.toString(), throwable);

        if (channelContext == null) {
          ReconnConf reconnConf = clientTioConfig.getReconnConf();
          if (reconnConf != null) {
            channelContext = new ClientChannelContext(clientTioConfig, asynchronousSocketChannel);
            channelContext.setServerNode(serverNode);
          }
        }

        if (!isReconnect) {
          if (channelContext != null) {
            attachment.setChannelContext(channelContext);
          }
        }

        boolean queued = false;
        try {
          queued = ReconnConf.put(channelContext);
        } catch (Throwable ignore) {
        }

        if (!queued) {
          com.litongjava.tio.core.Tio.close(channelContext, null, "No reconnect needed, close this connection", true,
              false, ChannelCloseCode.CLIENT_CONNECTION_FAIL);
        }
      }

    } catch (Throwable e) {
      log.error(e.toString(), e);

    } finally {
      // IMPORTANT: For SSL, beginHandshake must run before counting down connect
      // latch,
      // otherwise connect() may return too early.
      try {
        if (channelContext != null) {
          channelContext.setReconnect(isReconnect);

          if (SslUtils.isSsl(channelContext.tioConfig)) {
            if (isConnected) {
              // Create SslFacadeContext which constructs SSLFacade immediately (and reads TLS
              // peer attributes).
              ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler(channelContext);
              DecodeTask decodeTask = readCompletionHandler.getDecodeTask();
              SslFacadeContext sslFacadeContext = new SslFacadeContext(channelContext, decodeTask);
              sslFacadeContext.beginHandshake();
            } else {
              if (clientAioListener != null) {
                clientAioListener.onAfterConnected(channelContext, isConnected, isReconnect);
              }
            }
          } else {
            if (clientAioListener != null) {
              clientAioListener.onAfterConnected(channelContext, isConnected, isReconnect);
            }
          }

          // Update ipStats after connection attempt.
          TioConfig tioConfig = channelContext.tioConfig;
          if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
            for (Long v : tioConfig.ipStats.durationList) {
              IpStat ipStat = tioConfig.ipStats.get(v, channelContext);
              ipStat.getRequestCount().incrementAndGet();
              tioConfig.getIpStatListener().onAfterConnected(channelContext, isConnected, isReconnect, ipStat);
            }
          }
        }
      } catch (Throwable e1) {
        log.error(e1.toString(), e1);
      } finally {
        CountDownLatch latch = attachment.getCountDownLatch();
        if (latch != null) {
          latch.countDown();
        }
      }
    }
  }
}