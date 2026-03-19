package com.litongjava.tio.websocket.server;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;

/**
 *
 * @author tanyaowu 2017年7月30日 上午9:16:02
 */
public class WebSocketServerAioListener implements ServerAioListener {

  public WebSocketServerAioListener() {
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
    WebSocketSessionContext wsSessionContext = new WebSocketSessionContext();
    channelContext.set(wsSessionContext);
    return;
  }

  @Override
  public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {

  }

  @Override
  public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {
  }

  @Override
  public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {
  }

  @Override
  public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {

  }

  @Override
  public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {

  }

  @Override
  public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount) {
    return false;
  }

}
