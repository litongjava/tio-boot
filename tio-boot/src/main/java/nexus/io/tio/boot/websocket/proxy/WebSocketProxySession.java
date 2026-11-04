package nexus.io.tio.boot.websocket.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import nexus.io.tio.client.ClientChannelContext;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.websocket.common.Opcode;
import nexus.io.tio.websocket.common.WebSocketPacket;
import nexus.io.tio.websocket.common.WebSocketResponse;

/**
 * One proxied WebSocket connection: the client side, the upstream side and the
 * messages the client sent before the upstream handshake finished. Closing
 * either side closes both.
 */
public class WebSocketProxySession {
  public static final String ATTRIBUTE = "__websocket_proxy_session";

  private final ChannelContext client;
  private final ClientChannelContext upstream;
  private final String secWebSocketKey;
  private final Queue<WebSocketPacket> pending = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final List<WebSocketResponse> fragments = new ArrayList<>();
  private volatile boolean handshakeReceived;
  private volatile boolean open;

  public WebSocketProxySession(ChannelContext client, ClientChannelContext upstream, String secWebSocketKey) {
    this.client = client;
    this.upstream = upstream;
    this.secWebSocketKey = secWebSocketKey;
  }

  public static WebSocketProxySession of(ChannelContext channelContext) {
    return (WebSocketProxySession) channelContext.getAttribute(ATTRIBUTE);
  }

  /**
   * Sends a frame to the upstream, or queues it until the upstream handshake
   * has finished.
   */
  public synchronized void sendUpstream(WebSocketPacket packet) {
    if (closed.get()) {
      return;
    }
    if (!open) {
      pending.add(packet);
      return;
    }
    if (!Tio.send(upstream, packet)) {
      close("upstream send failed");
    }
  }

  /**
   * Sends a frame to the client.
   */
  public void sendClient(WebSocketPacket packet) {
    if (closed.get()) {
      return;
    }
    if (!Tio.send(client, packet)) {
      close("client send failed");
    }
  }

  /**
   * Called when the upstream accepted the handshake: flushes the queued frames.
   */
  public synchronized void onUpstreamOpen() {
    open = true;
    WebSocketPacket packet;
    while ((packet = pending.poll()) != null) {
      if (!Tio.send(upstream, packet)) {
        close("upstream send failed");
        return;
      }
    }
  }

  /** normal closure */
  public static final int CLOSE_NORMAL = 1000;
  /** the upstream failed or could not be reached */
  public static final int CLOSE_UPSTREAM_ERROR = 1011;

  /**
   * Closes both sides once with status 1000.
   *
   * @param reason reason for the log
   */
  public void close(String reason) {
    close(reason, CLOSE_NORMAL);
  }

  /**
   * Closes both sides once. The client receives a close frame with the given
   * status code first.
   *
   * @param reason reason for the log
   * @param code   close status code sent to the client
   */
  public void close(String reason, int code) {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    pending.clear();
    if (upstream != null) {
      Tio.remove(upstream, reason);
    }
    if (client != null && !client.isClosed && !client.isRemoved) {
      WebSocketResponse closeFrame = new WebSocketResponse();
      closeFrame.setWsOpcode(Opcode.CLOSE);
      closeFrame.setBody(new byte[] { (byte) (code >> 8), (byte) code });
      Tio.bSend(client, closeFrame);
      Tio.remove(client, reason);
    }
  }

  public boolean isClosed() {
    return closed.get();
  }

  public boolean isHandshakeReceived() {
    return handshakeReceived;
  }

  public void setHandshakeReceived(boolean handshakeReceived) {
    this.handshakeReceived = handshakeReceived;
  }

  public ChannelContext getClient() {
    return client;
  }

  public ClientChannelContext getUpstream() {
    return upstream;
  }

  public String getSecWebSocketKey() {
    return secWebSocketKey;
  }

  /**
   * Fragments of an upstream message that is not complete yet. Only touched by
   * the upstream decode thread.
   */
  List<WebSocketResponse> getFragments() {
    return fragments;
  }
}
