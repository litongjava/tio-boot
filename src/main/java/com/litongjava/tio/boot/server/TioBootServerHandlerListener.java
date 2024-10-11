package com.litongjava.tio.boot.server;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.HttpConst;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;

public class TioBootServerHandlerListener implements ServerAioListener {
  private ServerAioListener tcpListener = null;

  public TioBootServerHandlerListener() {
  }

  public TioBootServerHandlerListener(ServerAioListener tcpListener) {
    this.tcpListener = tcpListener;
  }

  @SuppressWarnings("deprecation")
  public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
    WebSocketSessionContext wsSessionContext = new WebSocketSessionContext();
    channelContext.set(wsSessionContext);
    if (tcpListener != null) {
      tcpListener.onAfterConnected(channelContext, isConnected, isReconnect);
    }

  }

  public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {
    if (tcpListener != null) {
      tcpListener.onAfterDecoded(channelContext, packet, packetSize);
    }

  }

  public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {
    if (tcpListener != null) {
      tcpListener.onAfterReceivedBytes(channelContext, receivedBytes);
    }
  }

  public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {
    if (packet instanceof HttpResponse) {
      HttpResponse httpResponse = (HttpResponse) packet;
      HttpRequest request = httpResponse.getHttpRequest();

      if (request != null) {
        String connection = request.getConnection();
        if (request.httpConfig.compatible1_0) {
          switch (request.requestLine.version) {
          case HttpConst.HttpVersion.V1_0:
            if (!HttpConst.RequestHeaderValue.Connection.keep_alive.equals(connection)) {
              Tio.remove(channelContext, "http request connection is not keep-alive：" + request.getRequestLine());
            }
            break;

          default:
            if (HttpConst.RequestHeaderValue.Connection.close.equals(connection)) {
              Tio.remove(channelContext, "http request connection is close：" + request.getRequestLine());
            }
            break;
          }
        } else {
          if (HttpConst.RequestHeaderValue.Connection.close.equals(connection)) {
            Tio.remove(channelContext, "http request Connection is close：" + request.getRequestLine());
          }
        }
      }
    }
    if (tcpListener != null) {
      tcpListener.onAfterSent(channelContext, packet, isSentSuccess);
    }

  }

  public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {
    if (tcpListener != null) {
      tcpListener.onAfterHandled(channelContext, packet, cost);
    }

  }

  /**
   * 连接关闭前触发本方法
   *
   * @param channelContext the channelcontext
   * @param throwable      the throwable 有可能为空
   * @param remark         the remark 有可能为空
   * @param isRemove
   * @throws Exception
   */

  public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
    if (tcpListener != null) {
      tcpListener.onBeforeClose(channelContext, throwable, remark, isRemove);
    }

  }

  /**
   * @param channelContext
   * @param interval              已经多久没有收发消息了，单位：毫秒
   * @param heartbeatTimeoutCount 心跳超时次数，第一次超时此值是1，以此类推。此值被保存在：channelContext.stat.heartbeatTimeoutCount
   * @return 返回true，那么服务器则不关闭此连接；返回false，服务器将按心跳超时关闭该连接
   */
  public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount) {
    if (tcpListener != null) {
      return tcpListener.onHeartbeatTimeout(channelContext, interval, heartbeatTimeoutCount);
    }
    return false;
  }
}