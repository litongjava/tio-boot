package com.litongjava.tio.boot.tcphandler;

import java.nio.ByteBuffer;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.ServerAioHandler;
import org.tio.websocket.server.WsServerAioHandler;

public class MultiProtocolHandler implements ServerAioHandler {
  private WsServerAioHandler wsServerHandler;;
  
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
