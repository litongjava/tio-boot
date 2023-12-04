package com.litongjava.tio.boot.server;

import java.nio.ByteBuffer;

import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpRequestDecoder;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.server.HttpServerAioHandler;
import org.tio.server.intf.ServerAioHandler;
import org.tio.websocket.common.WsRequest;
import org.tio.websocket.common.WsResponse;
import org.tio.websocket.common.WsSessionContext;
import org.tio.websocket.server.WsServerAioHandler;
import org.tio.websocket.server.WsServerConfig;
import org.tio.websocket.server.handler.IWsMsgHandler;

public class TioBootServerHandler implements ServerAioHandler {

  protected WsServerConfig wsServerConfig;
  private WsServerAioHandler wsServerAioHandler;
  protected HttpConfig httpConfig;
  private HttpServerAioHandler httpServerAioHandler;

  /**
   * @param wsServerConfig
   * @param wsMsgHandler
   */
  public TioBootServerHandler(WsServerConfig wsServerConfig, IWsMsgHandler wsMsgHandler, HttpConfig httpConfig,
      HttpRequestHandler requestHandler) {
    this.wsServerConfig = wsServerConfig;
    this.wsServerAioHandler = new WsServerAioHandler(wsServerConfig, wsMsgHandler);

    this.httpConfig = httpConfig;
    this.httpServerAioHandler = new HttpServerAioHandler(httpConfig, requestHandler);

  }

  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext)
      throws TioDecodeException {

    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    if (!wsSessionContext.isHandshaked()) {// 尚未握手
      HttpRequest request = HttpRequestDecoder.decode(buffer, limit, position, readableLength, channelContext,
          wsServerConfig);
      if (request == null) {
        return null;
      }
      if ("websocket".equals(request.getHeader("upgrade"))) {
        HttpResponse httpResponse = WsServerAioHandler.updateWebSocketProtocol(request, channelContext);
        if (httpResponse == null) {
          throw new TioDecodeException("http协议升级到websocket协议失败");
        }

        wsSessionContext.setHandshakeRequest(request);
        wsSessionContext.setHandshakeResponse(httpResponse);

        WsRequest wsRequestPacket = new WsRequest();
        // wsRequestPacket.setHeaders(httpResponse.getHeaders());
        // wsRequestPacket.setBody(httpResponse.getBody());
        wsRequestPacket.setHandShake(true);
        return wsRequestPacket;
      } else {
        channelContext.setAttribute(HttpServerAioHandler.REQUEST_KEY, request);
        return request;
      }
    } else {
      return wsServerAioHandler.decode(buffer, limit, position, readableLength, channelContext);
    }

  }

  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    if (packet instanceof WsResponse) {
      return wsServerAioHandler.encode(packet, tioConfig, channelContext);
    } else {
      return httpServerAioHandler.encode(packet, tioConfig, channelContext);
    }
  }

  public void handler(Packet packet, ChannelContext channelContext) throws Exception {
    if (packet instanceof WsRequest) {
      wsServerAioHandler.handler(packet, channelContext);
    } else {
      httpServerAioHandler.handler(packet, channelContext);
    }
  }
}