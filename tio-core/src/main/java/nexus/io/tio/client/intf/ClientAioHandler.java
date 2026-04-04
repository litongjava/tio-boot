package nexus.io.tio.client.intf;

import nexus.io.aio.Packet;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.intf.AioHandler;

/**
 *
 * @author tanyaowu
 * 2017年4月1日 上午9:14:24
 */
public interface ClientAioHandler extends AioHandler {
  /**
   * 创建心跳包
   * @return
   * @author tanyaowu
   */
  Packet heartbeatPacket(ChannelContext channelContext);
}
