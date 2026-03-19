package com.litongjava.tio.core;

import com.litongjava.aio.Packet;

/**
 * @author tanyaowu
 *
 */
public interface PacketConverter {
  /**
   * 
   * @param packet
   * @param channelContext 要发往的channelContext
   * @return
   * @author tanyaowu
   */
  public Packet convert(Packet packet, ChannelContext channelContext);
}
