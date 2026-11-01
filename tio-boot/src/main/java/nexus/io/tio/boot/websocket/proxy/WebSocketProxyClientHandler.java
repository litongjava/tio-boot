package nexus.io.tio.boot.websocket.proxy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import nexus.io.aio.BytePacket;
import nexus.io.aio.Packet;
import nexus.io.tio.client.intf.ClientAioHandler;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.TioConfig;
import nexus.io.tio.core.exception.TioDecodeException;
import nexus.io.tio.websocket.common.Opcode;
import nexus.io.tio.websocket.common.WebSocketClientDecoder;
import nexus.io.tio.websocket.common.WebSocketClientEncoder;
import nexus.io.tio.websocket.common.WebSocketPacket;
import nexus.io.tio.websocket.common.WebSocketRequest;
import nexus.io.tio.websocket.common.WebSocketResponse;

/**
 * Codec and handler of the upstream side of proxied WebSocket connections.
 * It reads the handshake response, then relays upstream frames to the client.
 */
public class WebSocketProxyClientHandler implements ClientAioHandler {

  private static final int MAX_HANDSHAKE_LENGTH = 16 * 1024;
  private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  @Override
  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext ctx)
      throws TioDecodeException {
    WebSocketProxySession session = WebSocketProxySession.of(ctx);
    if (session == null) {
      byte[] discard = new byte[readableLength];
      buffer.get(discard);
      return new BytePacket(discard);
    }
    if (!session.isHandshakeReceived()) {
      int headerEnd = -1;
      for (int i = position; i + 3 < limit; i++) {
        if (buffer.get(i) == '\r' && buffer.get(i + 1) == '\n' && buffer.get(i + 2) == '\r'
            && buffer.get(i + 3) == '\n') {
          headerEnd = i + 4;
          break;
        }
      }
      if (headerEnd < 0) {
        if (readableLength > MAX_HANDSHAKE_LENGTH) {
          throw new TioDecodeException("websocket handshake response too large");
        }
        return null;
      }
      byte[] head = new byte[headerEnd - position];
      buffer.get(head);
      // mark here, not in handler(): frames that follow in the same buffer are
      // decoded before the handler runs
      session.setHandshakeReceived(true);
      return new WebSocketHandshakePacket(new String(head, StandardCharsets.ISO_8859_1));
    }
    return WebSocketClientDecoder.decode(buffer, ctx);
  }

  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext ctx) {
    if (packet instanceof BytePacket) {
      byte[] bytes = ((BytePacket) packet).getBytes();
      ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
      buffer.put(bytes);
      return buffer;
    }
    return WebSocketClientEncoder.encode((WebSocketPacket) packet, tioConfig, ctx);
  }

  @Override
  public void handler(Packet packet, ChannelContext ctx) throws Exception {
    WebSocketProxySession session = WebSocketProxySession.of(ctx);
    if (session == null || session.isClosed()) {
      Tio.remove(ctx, "proxy session closed");
      return;
    }
    if (packet instanceof WebSocketHandshakePacket) {
      onHandshake(session, (WebSocketHandshakePacket) packet);
      return;
    }
    if (packet instanceof WebSocketResponse) {
      onFrame(session, (WebSocketResponse) packet);
    }
  }

  private void onHandshake(WebSocketProxySession session, WebSocketHandshakePacket response) throws Exception {
    int status = response.getStatus();
    if (status != 101) {
      session.close("upstream refused the websocket handshake with status " + status, WebSocketProxySession.CLOSE_UPSTREAM_ERROR);
      return;
    }
    String accept = response.getHeader("Sec-WebSocket-Accept");
    if (accept == null || !accept.equals(expectedAccept(session.getSecWebSocketKey()))) {
      session.close("invalid Sec-WebSocket-Accept from upstream", WebSocketProxySession.CLOSE_UPSTREAM_ERROR);
      return;
    }
    session.onUpstreamOpen();
  }

  private void onFrame(WebSocketProxySession session, WebSocketResponse frame) {
    Opcode opcode = frame.getWsOpcode();
    if (opcode == Opcode.PING) {
      WebSocketRequest pong = new WebSocketRequest();
      pong.setWsEof(true);
      pong.setWsOpcode(Opcode.PONG);
      pong.setBody(frame.getBody());
      session.sendUpstream(pong);
      return;
    }
    if (opcode == Opcode.PONG) {
      return;
    }
    if (opcode == Opcode.CLOSE) {
      session.close("upstream closed");
      return;
    }

    List<WebSocketResponse> fragments = session.getFragments();
    if (!frame.isWsEof()) {
      fragments.add(frame);
      return;
    }
    byte[] body = frame.getBody();
    if (!fragments.isEmpty()) {
      fragments.add(frame);
      opcode = fragments.get(0).getWsOpcode();
      body = join(fragments);
      fragments.clear();
    }

    WebSocketResponse out = new WebSocketResponse();
    out.setWsOpcode(opcode == Opcode.BINARY ? Opcode.BINARY : Opcode.TEXT);
    out.setBody(body == null ? new byte[0] : body);
    session.sendClient(out);
  }

  private static byte[] join(List<WebSocketResponse> parts) {
    int length = 0;
    for (WebSocketResponse part : parts) {
      if (part.getBody() != null) {
        length += part.getBody().length;
      }
    }
    byte[] all = new byte[length];
    int index = 0;
    for (WebSocketResponse part : parts) {
      byte[] body = part.getBody();
      if (body != null) {
        System.arraycopy(body, 0, all, index, body.length);
        index += body.length;
      }
    }
    return all;
  }

  static String expectedAccept(String key) throws Exception {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    byte[] digest = sha1.digest((key + GUID).getBytes(StandardCharsets.ISO_8859_1));
    return Base64.getEncoder().encodeToString(digest);
  }

  @Override
  public Packet heartbeatPacket(ChannelContext channelContext) {
    return null;
  }
}
