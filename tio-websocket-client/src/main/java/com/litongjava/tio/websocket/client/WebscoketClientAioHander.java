package com.litongjava.tio.websocket.client;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.tio.client.intf.ClientAioHandler;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.websocket.client.httpclient.HttpRequestEncoder;
import com.litongjava.tio.websocket.client.httpclient.HttpResponseDecoder;
import com.litongjava.tio.websocket.common.Opcode;
import com.litongjava.tio.websocket.common.WebSocketClientDecoder;
import com.litongjava.tio.websocket.common.WebSocketClientEncoder;
import com.litongjava.tio.websocket.common.WebSocketResponse;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;
import com.litongjava.tio.websocket.common.WebSocketPacket;

import io.reactivex.subjects.Subject;

public class WebscoketClientAioHander implements ClientAioHandler {
  private static final Logger log = LoggerFactory.getLogger(WebscoketClientAioHander.class);

  private static final String NOT_FINAL_WEBSOCKET_PACKET_PARTS = "TIO_N_F_W_P_P";

  @Override
  public Packet heartbeatPacket(ChannelContext ctx) {
    return null;
  }

  @Override
  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext ctx)
      throws TioDecodeException {
    WebSocketSessionContext session = (WebSocketSessionContext) ctx.get();
    if (!session.isHandshaked()) {
      HttpResponse response = HttpResponseDecoder.decode(buffer, limit, position, readableLength, ctx);
      session.setHandshakeResponse(response);
      return response;
    }
    WebSocketResponse packet = WebSocketClientDecoder.decode(buffer, ctx);
    if (packet != null) {
      if (!packet.isWsEof()) { // 数据包尚未完成
        @SuppressWarnings("unchecked")
        List<WebSocketResponse> parts = (List<WebSocketResponse>) ctx.getAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS);
        if (parts == null) {
          parts = new ArrayList<>();
          ctx.setAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS, parts);
        }
        parts.add(packet);
      } else {
        @SuppressWarnings("unchecked")
        List<WebSocketResponse> parts = (List<WebSocketResponse>) ctx.getAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS);
        if (parts != null) {
          ctx.setAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS, null);

          parts.add(packet);
          WebSocketResponse first = parts.get(0);
          packet.setWsOpcode(first.getWsOpcode());

          int allBodyLength = 0;
          for (WebSocketResponse wsRequest : parts) {
            allBodyLength += wsRequest.getBody().length;
          }

          byte[] allBody = new byte[allBodyLength];
          Integer index = 0;
          for (WebSocketResponse wsRequest : parts) {
            System.arraycopy(wsRequest.getBody(), 0, allBody, index, wsRequest.getBody().length);
            index += wsRequest.getBody().length;
          }
          packet.setBody(allBody);
        }

        HttpRequest handshakeRequest = session.getHandshakeRequest();
        if (packet.getWsOpcode() != Opcode.BINARY) {
          try {
            String text = new String(packet.getBody(), handshakeRequest.getCharset());
            packet.setWsBodyText(text);
          } catch (UnsupportedEncodingException e) {
            log.error(e.toString(), e);
          }
        }
      }
    }
    return packet;
  }

  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext ctx) {
    WebSocketSessionContext session = (WebSocketSessionContext) ctx.get();
    if (!session.isHandshaked() && packet instanceof HttpRequest) {
      try {
        return HttpRequestEncoder.encode((HttpRequest) packet, tioConfig, ctx);
      } catch (UnsupportedEncodingException e) {
        log.error(e.toString());
        return null;
      }
    }
    try {
      return WebSocketClientEncoder.encode((WebSocketPacket) packet, tioConfig, ctx);
    } catch (Exception e) {
      log.error(e.toString());
      return null;
    }
  }

  @Override
  public void handler(Packet packet, ChannelContext ctx) throws Exception {
    if (packet instanceof WebSocketPacket) {
      WebSocketPacket wsPacket = (WebSocketPacket) packet;
      if (!wsPacket.isWsEof()) {
        return;
      }
    }
    @SuppressWarnings("unchecked")
    Subject<Packet> packetPublisher = (Subject<Packet>) ctx.getAttribute(WebSocketImpl.packetPublisherKey);
    packetPublisher.onNext(packet);
  }
}
