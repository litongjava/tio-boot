package nexus.io.tio.boot.websocket.proxy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Target of a proxied WebSocket connection: a {@code ws://host:port/path?query}
 * url and the headers to send in the upstream handshake.
 */
public class WebSocketUpstream {
  private final String url;
  private final Map<String, String> headers = new LinkedHashMap<>();

  public WebSocketUpstream(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public WebSocketUpstream header(String name, String value) {
    if (name != null && value != null) {
      headers.put(name, value);
    }
    return this;
  }
}
