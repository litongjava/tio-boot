package nexus.io.tio.boot.websocket.proxy;

import nexus.io.aio.Packet;

/**
 * Status line and headers of the upstream handshake response.
 */
public class WebSocketHandshakePacket extends Packet {
  private static final long serialVersionUID = 1L;

  private final String head;

  public WebSocketHandshakePacket(String head) {
    this.head = head;
  }

  public String getHead() {
    return head;
  }

  /**
   * @return the status code of the status line, or -1 if it cannot be parsed
   */
  public int getStatus() {
    int lineEnd = head.indexOf("\r\n");
    String statusLine = lineEnd < 0 ? head : head.substring(0, lineEnd);
    String[] parts = statusLine.split(" ");
    if (parts.length < 2) {
      return -1;
    }
    try {
      return Integer.parseInt(parts[1].trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * @param name header name, case-insensitive
   * @return the header value, or null
   */
  public String getHeader(String name) {
    String[] lines = head.split("\r\n");
    for (int i = 1; i < lines.length; i++) {
      int colon = lines[i].indexOf(':');
      if (colon > 0 && lines[i].substring(0, colon).trim().equalsIgnoreCase(name)) {
        return lines[i].substring(colon + 1).trim();
      }
    }
    return null;
  }
}
