package com.litongjava.tio.core.ssl;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.ChannelCloseCode;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.ssl.facade.ISessionClosedListener;

public class SslSessionClosedListener implements ISessionClosedListener {
  private ChannelContext channelContext;

  public SslSessionClosedListener(ChannelContext channelContext) {
    this.channelContext = channelContext;
  }

  @Override
  public void onSessionClosed() {
    // log.info("{} onSessionClosed", channelContext);
    Tio.close(channelContext, "SSL SessionClosed", ChannelCloseCode.SSL_SESSION_CLOSED);
  }

}
