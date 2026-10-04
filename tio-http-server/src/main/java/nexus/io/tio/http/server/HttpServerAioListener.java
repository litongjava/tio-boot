package nexus.io.tio.http.server;

import nexus.io.aio.Packet;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.server.intf.ServerAioListener;

/**
 * HTTP ServerAioListener
 * @author tanyaowu
 */
public class HttpServerAioListener implements ServerAioListener {

  public HttpServerAioListener() {
  }

  @Override
  public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) {
    return;
  }

  @Override
  public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) {

  }

  @Override
  public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) {
    // The encoder resolves connection policy; the core send-completion path
    // closes after the entire response (including file data) has been written.
  }

  @Override
  public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) {
    // HttpRequest request = (HttpRequest) channelContext.getAttribute(HttpServerAioHandler.REQUEST_KEY);
    // if (request != null) {
    // request.setClosed(true);
    // }
  }

  @Override
  public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {

  }

  @Override
  public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {

  }

  @Override
  public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount) {
    return false;
  }
}
