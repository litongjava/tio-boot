package nexus.io.tio.core.ssl;

import nexus.io.tio.core.ChannelCloseCode;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.ssl.facade.ISessionClosedListener;

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
