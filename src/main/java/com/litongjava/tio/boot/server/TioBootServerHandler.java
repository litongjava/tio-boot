package com.litongjava.tio.boot.server;

import java.nio.ByteBuffer;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.core.intf.Packet;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpRequestDecoder;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.HttpServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.websocket.common.WsRequest;
import com.litongjava.tio.websocket.common.WsResponse;
import com.litongjava.tio.websocket.common.WsSessionContext;
import com.litongjava.tio.websocket.server.WsServerAioHandler;
import com.litongjava.tio.websocket.server.WsServerConfig;
import com.litongjava.tio.websocket.server.handler.IWsMsgHandler;

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