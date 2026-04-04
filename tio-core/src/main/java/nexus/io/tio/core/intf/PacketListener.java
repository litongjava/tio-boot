package nexus.io.tio.core.intf;

import nexus.io.aio.Packet;
import nexus.io.tio.core.ChannelContext;

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
