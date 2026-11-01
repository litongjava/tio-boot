package nexus.io.tio.core.ssl;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.aio.Packet;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.ssl.facade.ISSLListener;
import nexus.io.tio.core.task.SendPacketTask;

/**
 * @author tanyaowu
 *
 */
public class SslListener implements ISSLListener {
  private static Logger log = LoggerFactory.getLogger(SslListener.class);

  private ChannelContext channelContext = null;

  /**
   * 
   */
  public SslListener(ChannelContext channelContext) {
    this.channelContext = channelContext;
  }

  @Override
  public void onWrappedData(SslVo sslVo) {
    //log.info("{}, Received data after SSL encryption, ready to be sent out {}", channelContext, sslVo);

    Object obj = sslVo.getObj();
    if (obj == null) { // 如果是null，则是握手尚未完成时的数据
      Packet p = new Packet();

      p.setPreEncodedByteBuffer(sslVo.getByteBuffer());
      p.setSslEncrypted(true);

      new SendPacketTask(channelContext).sendPacket(p);

    }

  }

  @Override
  public void onPlainData(ByteBuffer plainBuffer) {
    // Data decrypted before the handshake completes is held, not dropped.
    channelContext.sslFacadeContext.onPlainData(plainBuffer);
  }

}
