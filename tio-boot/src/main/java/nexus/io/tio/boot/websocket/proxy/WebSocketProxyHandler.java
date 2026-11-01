package nexus.io.tio.boot.websocket.proxy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.utils.HttpIpUtils;
import nexus.io.tio.websocket.common.Opcode;
import nexus.io.tio.websocket.common.WebSocketRequest;
import nexus.io.tio.websocket.server.handler.IWebSocketHandler;

/**
 * WebSocket reverse proxy. Register it on the WebSocketRouter for the paths to
 * proxy; for each client connection the resolver picks the upstream, the
 * handler connects to it and relays text and binary messages both ways.
 *
 * <pre>
 * router.add("/ws/**", new WebSocketProxyHandler((request, ctx) -&gt;
 *     WebSocketProxyHandler.upstreamOf("ws://127.0.0.1:8081" + request.getRequestLine().getPathAndQuery(), request)));
 * </pre>
 */
public class WebSocketProxyHandler implements IWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(WebSocketProxyHandler.class);

  private static final String UPSTREAM_ATTRIBUTE = "__websocket_proxy_upstream";

  /** handshake headers that are generated for the upstream connection */
  private static final Set<String> SKIP_HEADERS = new HashSet<>(Arrays.asList("host", "upgrade", "connection",
      "sec-websocket-key", "sec-websocket-version", "sec-websocket-extensions", "sec-websocket-accept",
      "content-length", "transfer-encoding", "keep-alive", "te", "trailer", "proxy-connection"));

  private final WebSocketUpstreamResolver resolver;
  private final WebSocketProxyClient client;

  public WebSocketProxyHandler(WebSocketUpstreamResolver resolver) {
    this(resolver, WebSocketProxyClient.me());
  }

  public WebSocketProxyHandler(WebSocketUpstreamResolver resolver, WebSocketProxyClient client) {
    this.resolver = resolver;
    this.client = client;
  }

  /**
   * Builds an upstream that carries over the client's handshake headers
   * (cookies, authorization, custom headers) and adds X-Forwarded-For.
   *
   * @param url         upstream ws:// url including query
   * @param httpRequest handshake request of the client
   * @return the upstream
   */
  public static WebSocketUpstream upstreamOf(String url, HttpRequest httpRequest) {
    WebSocketUpstream upstream = new WebSocketUpstream(url);
    for (Map.Entry<String, String> e : httpRequest.getHeaders().entrySet()) {
      if (e.getKey() != null && !SKIP_HEADERS.contains(e.getKey().toLowerCase(Locale.ROOT))) {
        upstream.header(e.getKey(), e.getValue());
      }
    }
    String forwardedFor = httpRequest.getHeader("x-forwarded-for");
    upstream.header("x-forwarded-for", forwardedFor == null ? HttpIpUtils.getRealIp(httpRequest) : forwardedFor);
    String host = httpRequest.getHeader("host");
    if (host != null) {
      upstream.header("x-forwarded-host", host);
    }
    return upstream;
  }

  @Override
  public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext)
      throws Exception {
    WebSocketUpstream upstream;
    try {
      upstream = resolver.resolve(httpRequest, channelContext);
    } catch (Exception e) {
      log.error("resolve websocket upstream failed: {}", e.getMessage());
      return null;
    }
    if (upstream == null) {
      return null;
    }
    channelContext.setAttribute(UPSTREAM_ATTRIBUTE, upstream);
    return httpResponse;
  }

  @Override
  public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext)
      throws Exception {
    WebSocketUpstream upstream = (WebSocketUpstream) channelContext.getAttribute(UPSTREAM_ATTRIBUTE);
    if (upstream == null) {
      Tio.remove(channelContext, "no websocket upstream");
      return;
    }
    client.open(channelContext, upstream);
  }

  @Override
  public Object onText(WebSocketRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
    WebSocketRequest packet = new WebSocketRequest();
    packet.setWsEof(true);
    packet.setWsOpcode(Opcode.TEXT);
    packet.setBody(text.getBytes(StandardCharsets.UTF_8));
    forward(channelContext, packet);
    return null;
  }

  @Override
  public Object onBytes(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    forward(channelContext, WebSocketRequest.fromBytes(bytes));
    return null;
  }

  @Override
  public Object onClose(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WebSocketProxySession session = WebSocketProxySession.of(channelContext);
    if (session != null) {
      session.close("client closed");
    }
    return null;
  }

  private void forward(ChannelContext channelContext, WebSocketRequest packet) {
    WebSocketProxySession session = WebSocketProxySession.of(channelContext);
    if (session == null || session.isClosed()) {
      Tio.remove(channelContext, "websocket upstream unavailable");
      return;
    }
    session.sendUpstream(packet);
  }
}
