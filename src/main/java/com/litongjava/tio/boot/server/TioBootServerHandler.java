package com.litongjava.tio.boot.server;

import java.nio.ByteBuffer;

import com.litongjava.aio.Packet;
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
import com.litongjava.tio.websocket.common.WebscoketResponse;
import com.litongjava.tio.websocket.common.WebsocketRequest;
import com.litongjava.tio.websocket.common.WebsocketSessionContext;
import com.litongjava.tio.websocket.server.WebsocketServerAioHandler;
import com.litongjava.tio.websocket.server.WebsocketServerConfig;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

public class TioBootServerHandler implements ServerAioHandler {

  /**
   * 请求行，例如 GET / HTTP/1.1 至少一个头部字段，例如 Host: www.example.com 头部和消息体之间的空行（CRLF）
   * 类似地，一个最基本的HTTP响应头包括：
   * 
   * 状态行，例如 HTTP/1.1 200 OK 至少一个头部字段 头部和消息体之间的空行（CRLF） 考虑到这些，一个非常保守的估计可能是这样的：
   * 
   * 请求行/状态行：大约20字节（这是一个非常紧凑的行，实际通常会更长） 至少一个头部字段：比如 Host: 后面跟一个短域名，大约20字节
   * CRLF（\r\n）作为行分隔符，2字节 头部和消息体之间的空行（CRLF），2字节 因此，一个非常保守的估计可能是44字节（20 + 20 + 2 +
   * 2）
   * 
   * 在window执行下面的命令发送的http请求长度是73个字节 curl http://localhost/
   * 
   * 这里为了保险,采用最保守的方式,设置为44的字节
   */

  public static final int minimumHttpHeaderLength = 44;

  protected WebsocketServerConfig wsServerConfig;
  private WebsocketServerAioHandler wsServerAioHandler;
  protected HttpConfig httpConfig;
  private HttpServerAioHandler httpServerAioHandler;
  private ServerAioHandler serverAioHandler;

  /**
   * @param wsServerConfig
   * @param wsMsgHandler
   * @param serverTcpHandler
   */
  public TioBootServerHandler(WebsocketServerConfig wsServerConfig, IWebSocketHandler wsMsgHandler, HttpConfig httpConfig, ITioHttpRequestHandler requestHandler, ServerAioHandler serverAioHandler) {
    this.wsServerConfig = wsServerConfig;
    this.wsServerAioHandler = new WebsocketServerAioHandler(wsServerConfig, wsMsgHandler);

    this.httpConfig = httpConfig;
    this.httpServerAioHandler = new HttpServerAioHandler(httpConfig, requestHandler);
    this.serverAioHandler = serverAioHandler;
  }

  public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws Exception {

    WebsocketSessionContext wsSessionContext = (WebsocketSessionContext) channelContext.get();
    if (wsSessionContext.isHandshaked()) {// WebSocket已经握手
      return wsServerAioHandler.decode(buffer, limit, position, readableLength, channelContext);
    } else {
      if (readableLength < minimumHttpHeaderLength) {
        // 数据或许不足以解析为Http协议
        if (serverAioHandler != null) {
          return serverAioHandler.decode(buffer, limit, 0, readableLength, channelContext);
        }
      }
      HttpRequest request = null;
      try {
        request = HttpRequestDecoder.decode(buffer, limit, position, readableLength, channelContext, httpConfig);
      } catch (TioDecodeException e) {
        if (serverAioHandler == null) {
          e.printStackTrace();
          return null;
        }
      }
      if (request == null) {
        if (serverAioHandler != null) {
          buffer.position(0);
          return serverAioHandler.decode(buffer, limit, 0, readableLength, channelContext);
        } else {
          return null;
        }
      }
      if ("websocket".equals(request.getHeader("upgrade"))) {
        HttpResponse httpResponse = WebsocketServerAioHandler.updateWebSocketProtocol(request, channelContext);
        if (httpResponse == null) {
          throw new TioDecodeException("Failed to upgrade HTTP protocol to WebSocket protocol.");
        }

        wsSessionContext.setHandshakeRequest(request);
        wsSessionContext.setHandshakeResponse(httpResponse);
        WebsocketRequest wsRequestPacket = new WebsocketRequest();
        wsRequestPacket.setHandShake(true);
        return wsRequestPacket;
      } else {
        channelContext.setAttribute(HttpServerAioHandler.REQUEST_KEY, request);
        return request;
      }
    }

  }

  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    if (packet instanceof HttpResponse) {
      return httpServerAioHandler.encode(packet, tioConfig, channelContext);

    } else if (packet instanceof HttpResponsePacket) {
      HttpResponsePacket responsePacket = (HttpResponsePacket) packet;
      return responsePacket.toByteBuffer(tioConfig);

    } else if (packet instanceof WebscoketResponse) {
      return wsServerAioHandler.encode(packet, tioConfig, channelContext);
    } else {
      return serverAioHandler.encode(packet, tioConfig, channelContext);
    }
  }

  public void handler(Packet packet, ChannelContext channelContext) throws Exception {
    if (packet instanceof HttpRequest) {
      httpServerAioHandler.handler(packet, channelContext);
    } else if (packet instanceof WebsocketRequest) {
      wsServerAioHandler.handler(packet, channelContext);
    } else {
      serverAioHandler.handler(packet, channelContext);
    }
  }
}