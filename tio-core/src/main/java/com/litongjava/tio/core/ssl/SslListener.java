package com.litongjava.tio.core.ssl;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.ssl.facade.ISSLListener;
import com.litongjava.tio.core.task.DecodeTask;
import com.litongjava.tio.core.task.SendPacketTask;

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
    // This is the deciphered payload for your app to consume
    SslFacadeContext sslFacadeContext = channelContext.sslFacadeContext;
    DecodeTask decodeTask = sslFacadeContext.getDecodeTask();

    boolean handshakeCompleted = sslFacadeContext.isHandshakeCompleted();
    if (handshakeCompleted) {
      log.debug(
          "{}, After receiving the data decrypted by SSL, the SSL handshake is complete and ready to be decoded，{}, isSSLHandshakeCompleted {}",
          channelContext, plainBuffer, handshakeCompleted);
      decodeTask.decode(channelContext, plainBuffer);
    } else {
      log.info(
          "{}, SSL decrypted data is received, but the SSL handshake is not complete，{}, isSSLHandshakeCompleted {}",
          channelContext, plainBuffer, handshakeCompleted);
    }
  }

}
