package com.litongjava.tio.core.intf;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelContext;

/**
 * @author tanyaowu
 * 2017年5月8日 下午1:14:08
 */
public interface PacketListener {
  /**
   *
   * @param channelContext
   * @param packet
   * @param isSentSuccess
   * @throws Exception
   * @author tanyaowu
   */
  void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception;

}
