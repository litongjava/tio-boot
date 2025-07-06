package com.litongjava.tio.boot.server;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.litongjava.aio.ByteBufferPacket;
import com.litongjava.aio.BytePacket;
import com.litongjava.aio.Packet;
import com.litongjava.aio.StringPacket;
import com.litongjava.tio.boot.decode.TioDecodeExceptionHandler;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpRequestDecoder;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponsePacket;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.server.HttpServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.websocket.common.WebSocketRequest;
import com.litongjava.tio.websocket.common.WebSocketResponse;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;
import com.litongjava.tio.websocket.server.WebsocketServerAioHandler;
import com.litongjava.tio.websocket.server.WebsocketServerConfig;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioBootServerHandler implements ServerAioHandler {

  /**
   * Minimum HTTP header length estimate.
   * 
   * Example of a basic HTTP request:
   * - Request line: "GET / HTTP/1.1" (14 bytes)
   * - At least one header field: "Host: 127.0.0.1" (15)
   * - CRLF as line delimiter: 2 bytes
   * - CRLF between headers and body: 2 bytes
   * 
   * Total estimated minimum: 33 bytes
   * 
   * Note: Actual HTTP requests may be longer.
   */
  public static final int MINIMUM_HTTP_HEADER_LENGTH = 32;

  protected WebsocketServerConfig defaultServerConfig;
  private WebsocketServerAioHandler defaultServerAioHandler;
  protected HttpConfig httpConfig;
  private HttpServerAioHandler httpServerAioHandler;
  private ServerAioHandler serverAioHandler;
  private TioDecodeExceptionHandler tioDecodeExceptionHandler;

  /**
   * Constructor to initialize the TioBootServerHandler.
   * 
   * @param wsServerConfig          WebSocket server configuration.
   * @param websocketHandler        WebSocket handler.
   * @param httpConfig              HTTP configuration.
   * @param requestHandler          HTTP request handler.
   * @param serverAioHandler        Additional server AIO handler.
   * @param tioDecodeExceptionHandler Exception handler for decode errors.
   */
  public TioBootServerHandler(WebsocketServerConfig wsServerConfig, IWebSocketHandler websocketHandler, HttpConfig httpConfig, ITioHttpRequestHandler requestHandler, ServerAioHandler serverAioHandler,
      TioDecodeExceptionHandler tioDecodeExceptionHandler) {
    this.defaultServerConfig = wsServerConfig;
    this.defaultServerAioHandler = new WebsocketServerAioHandler(wsServerConfig, websocketHandler);

    this.httpConfig = httpConfig;
    this.httpServerAioHandler = new HttpServerAioHandler(httpConfig, requestHandler);
    this.serverAioHandler = serverAioHandler;
    this.tioDecodeExceptionHandler = tioDecodeExceptionHandler;
  }

  /**
   * Decodes incoming ByteBuffer into a Packet.
   * 
   * @param buffer          The ByteBuffer containing incoming data.
   * @param limit           The limit of the buffer.
   * @param position        The current position in the buffer.
   * @param readableLength  The number of readable bytes.
   * @param channelContext  The context of the channel.
   * @return                The decoded Packet or null if insufficient data.
   * @throws Exception      If an error occurs during decoding.
   */
  @Override
  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws Exception {

    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    if (wsSessionContext.isHandshaked()) { // WebSocket handshake completed
      return defaultServerAioHandler.decode(buffer, limit, position, readableLength, channelContext);
    } else {
      if (readableLength < MINIMUM_HTTP_HEADER_LENGTH) {
        // Data might be insufficient to parse as HTTP protocol
        if (serverAioHandler != null) {
          return serverAioHandler.decode(buffer, limit, position, readableLength, channelContext);
        }
        return null;
      }

      HttpRequest request;
      try {
        request = HttpRequestDecoder.decode(buffer, limit, position, readableLength, channelContext, httpConfig);
      } catch (TioDecodeException e) {
        if (serverAioHandler != null) {
          return serverAioHandler.decode(buffer, limit, position, readableLength, channelContext);
        }
        if (tioDecodeExceptionHandler != null) {
          tioDecodeExceptionHandler.handle(buffer, channelContext, httpConfig, e);
        } else {
          log.error("Decode exception occurred", e);
        }
        return null;
      }

      if (request == null) {
        return null;
      }
      String upgradeHeader = request.getHeader("upgrade");
      if ("websocket".equalsIgnoreCase(upgradeHeader)) {
        HttpResponse httpResponse = WebsocketServerAioHandler.upgradeWebSocketProtocol(request, channelContext);
        if (httpResponse == null) {
          throw new TioDecodeException("Failed to upgrade HTTP protocol to WebSocket protocol.");
        }

        wsSessionContext.setHandshakeRequest(request);
        wsSessionContext.setHandshakeResponse(httpResponse);
        WebSocketRequest wsRequestPacket = new WebSocketRequest();
        wsRequestPacket.setHandShake(true);
        return wsRequestPacket;
      } else {
        channelContext.setAttribute(HttpServerAioHandler.REQUEST_KEY, request);
        return request;
      }
    }
  }

  /**
   * Encodes a Packet into a ByteBuffer.
   * 
   * @param packet          The Packet to encode.
   * @param tioConfig       The Tio configuration.
   * @param channelContext  The context of the channel.
   * @return                The encoded ByteBuffer.
   */
  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    if (packet instanceof HttpResponse) {
      return httpServerAioHandler.encode(packet, tioConfig, channelContext);
    } else if (packet instanceof HttpResponsePacket) {
      HttpResponsePacket responsePacket = (HttpResponsePacket) packet;
      return responsePacket.toByteBuffer(tioConfig);
    } else if (packet instanceof WebSocketResponse) {
      return defaultServerAioHandler.encode(packet, tioConfig, channelContext);
    } else {
      if (serverAioHandler != null) {
        return serverAioHandler.encode(packet, tioConfig, channelContext);

      } else if (packet instanceof BytePacket) {
        byte[] bytes = ((BytePacket) packet).getBytes();
        return ByteBuffer.wrap(bytes);

      } else if (packet instanceof ByteBufferPacket) {
        return ((ByteBufferPacket) packet).getByteBuffer();

      } else if (packet instanceof StringPacket) {
        byte[] bytes;
        try {
          bytes = ((StringPacket) packet).getBody().getBytes(tioConfig.getCharset());
          return ByteBuffer.wrap(bytes);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
      log.warn("Unknown packet type: {}", packet.getClass().getName());
      return null;
    }
  }

  /**
   * Handles a received Packet.
   * 
   * @param packet          The received Packet.
   * @param channelContext  The context of the channel.
   * @throws Exception      If an error occurs during handling.
   */
  @Override
  public void handler(Packet packet, ChannelContext channelContext) throws Exception {
    if (packet instanceof HttpRequest) {
      httpServerAioHandler.handler(packet, channelContext);
    } else if (packet instanceof WebSocketRequest) {
      defaultServerAioHandler.handler(packet, channelContext);
    } else {
      if (serverAioHandler != null) {
        serverAioHandler.handler(packet, channelContext);
      } else {
        log.warn("No handler available for packet type: {}", packet.getClass().getName());
      }
    }
  }
}
