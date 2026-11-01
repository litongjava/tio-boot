package nexus.io.tio.boot.websocket.proxy;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.http.common.HttpRequest;

/**
 * Chooses the upstream of a WebSocket connection during the handshake.
 */
@FunctionalInterface
public interface WebSocketUpstreamResolver {

  /**
   * @param httpRequest    handshake request of the client
   * @param channelContext client connection
   * @return the upstream, or null to refuse the connection
   * @throws Exception on failure, the connection is refused
   */
  WebSocketUpstream resolve(HttpRequest httpRequest, ChannelContext channelContext) throws Exception;
}
