package com.litongjava.tio.boot.tcp.handler;

import java.nio.ByteBuffer;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.core.intf.Packet;
import com.litongjava.tio.server.intf.ServerAioHandler;

public class MultiProtocolHandler implements ServerAioHandler {

  public MultiProtocolHandler(MultiProcotolConfig multiProcotolConfig) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext)
      throws TioDecodeException {
    return null;
  }

  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    return null;
  }

  @Override
  public void handler(Packet packet, ChannelContext channelContext) throws Exception {

  }

}
