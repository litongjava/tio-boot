package nexus.io.tio.boot.websocket.proxy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.aio.BytePacket;
import nexus.io.tio.boot.server.ChannelCloseHooks;
import nexus.io.tio.client.ClientChannelContext;
import nexus.io.tio.client.ClientTioConfig;
import nexus.io.tio.client.ConnectionCompletionHandler;
import nexus.io.tio.client.ConnectionCompletionVo;
import nexus.io.tio.client.TioClient;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.DefaultAioListener;
import nexus.io.tio.core.Node;
import nexus.io.tio.core.Tio;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Opens upstream WebSocket connections for {@link WebSocketProxyHandler}. All
 * upstream connections share one TioClient, so a gateway can hold many proxied
 * connections without a thread pool per connection.
 */
public class WebSocketProxyClient {
  private static final Logger log = LoggerFactory.getLogger(WebSocketProxyClient.class);

  /** upstream connect timeout in seconds, default 5 */
  public static final String CONNECT_TIMEOUT_KEY = "server.websocket.proxy.connect.timeout";

  private static volatile WebSocketProxyClient me;

  private final SecureRandom random = new SecureRandom();
  private final ClientTioConfig config;
  private final TioClient tioClient;

  public static WebSocketProxyClient me() {
    if (me == null) {
      synchronized (WebSocketProxyClient.class) {
        if (me == null) {
          me = new WebSocketProxyClient();
        }
      }
    }
    return me;
  }

  public WebSocketProxyClient() {
    config = new ClientTioConfig(new WebSocketProxyClientHandler(), new UpstreamListener());
    config.setName("websocket-proxy");
    config.setHeartbeatTimeout(0);
    config.setConnectionCompletionHandler(new ConnectionCompletionHandler() {
      @Override
      public void failed(Throwable error, ConnectionCompletionVo attachment) {
        try {
          attachment.getAsynchronousSocketChannel().close();
        } catch (IOException ignored) {
          // already closed
        } finally {
          if (attachment.getCountDownLatch() != null) {
            attachment.getCountDownLatch().countDown();
          }
        }
        ClientChannelContext upstream = attachment.getChannelContext();
        WebSocketProxySession session = upstream == null ? null : WebSocketProxySession.of(upstream);
        if (session != null) {
          session.close("upstream connect failed: " + error.getMessage(), WebSocketProxySession.CLOSE_UPSTREAM_ERROR);
        }
      }
    });
    try {
      tioClient = new TioClient(config);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create the websocket proxy client", e);
    }
  }

  /**
   * Connects to the upstream and sends the handshake. Frames the client sends
   * meanwhile are queued in the session.
   *
   * @param client      client connection, already handshaked
   * @param upstreamDef upstream url and headers
   * @return the session
   */
  public WebSocketProxySession open(ChannelContext client, WebSocketUpstream upstreamDef) {
    URI uri = URI.create(upstreamDef.getUrl());
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!"ws".equals(scheme) && !"http".equals(scheme)) {
      throw new IllegalArgumentException("Only ws:// upstreams are supported: " + upstreamDef.getUrl());
    }
    int port = uri.getPort() > 0 ? uri.getPort() : 80;
    Node node = new Node(uri.getHost(), port);

    String key = newKey();
    ClientChannelContext upstream = new ClientChannelContext(config, null);
    WebSocketProxySession session = new WebSocketProxySession(client, upstream, key);
    upstream.setAttribute(WebSocketProxySession.ATTRIBUTE, session);
    client.setAttribute(WebSocketProxySession.ATTRIBUTE, session);
    ChannelCloseHooks.add(client, () -> session.close("client disconnected"));
    if (client.isClosed || client.isRemoved || client.isWaitingClose) {
      session.close("client already closed");
      return session;
    }

    try {
      tioClient.connect(node, null, 0, upstream, EnvUtils.getInt(CONNECT_TIMEOUT_KEY, 5));
    } catch (Exception e) {
      log.error("connect {} failed: {}", upstreamDef.getUrl(), e.getMessage());
      session.close("upstream connect failed", WebSocketProxySession.CLOSE_UPSTREAM_ERROR);
      return session;
    }
    if (session.isClosed() || upstream.isClosed || upstream.isRemoved || upstream.asynchronousSocketChannel == null) {
      session.close("upstream connect failed", WebSocketProxySession.CLOSE_UPSTREAM_ERROR);
      return session;
    }

    byte[] handshake = buildHandshake(uri, port, key, upstreamDef.getHeaders());
    if (!Tio.send(upstream, new BytePacket(handshake))) {
      session.close("upstream handshake send failed", WebSocketProxySession.CLOSE_UPSTREAM_ERROR);
    }
    return session;
  }

  private String newKey() {
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  private static byte[] buildHandshake(URI uri, int port, String key, Map<String, String> headers) {
    String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
    if (uri.getRawQuery() != null) {
      path = path + "?" + uri.getRawQuery();
    }
    StringBuilder sb = new StringBuilder(256);
    sb.append("GET ").append(path).append(" HTTP/1.1\r\n");
    sb.append("Host: ").append(uri.getHost());
    if (port != 80) {
      sb.append(':').append(port);
    }
    sb.append("\r\n");
    sb.append("Upgrade: websocket\r\n");
    sb.append("Connection: Upgrade\r\n");
    sb.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
    sb.append("Sec-WebSocket-Version: 13\r\n");
    for (Map.Entry<String, String> e : headers.entrySet()) {
      sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
    }
    sb.append("\r\n");
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Closes the session when the upstream connection closes.
   */
  private static class UpstreamListener extends DefaultAioListener {
    @Override
    public void onBeforeClose(ChannelContext context, Throwable error, String reason, boolean remove) {
      WebSocketProxySession session = WebSocketProxySession.of(context);
      if (session != null) {
        session.close("upstream disconnected");
      }
    }
  }
}
