package com.litongjava.tio.boot.tcp.handler;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.intf.Packet;
import com.litongjava.tio.server.intf.ServerAioListener;

public class MultiProtocolListener implements ServerAioListener {

  @Override
  public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount) {
    // TODO Auto-generated method stub
    return false;
  }

}
