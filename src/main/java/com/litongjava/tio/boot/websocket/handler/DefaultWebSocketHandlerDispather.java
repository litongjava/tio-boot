package com.litongjava.tio.boot.websocket.handler;

import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.websocket.common.WsRequest;
import com.litongjava.tio.websocket.common.WsSessionContext;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * dispather
 * @author Tong Li
 *
 */
@Slf4j
public class DefaultWebSocketHandlerDispather implements IWebSocketHandler {

  /**
   * 握手时走这个方法，业务可以在这里获取cookie，request参数等
   */
  @Override
  public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) throws Exception {
    String path = httpRequest.getRequestLine().getPath();
    WebSocketRouter webSocketRouter = TioBootServer.me().getWebSocketRouter();
    if (webSocketRouter == null) {
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.handshake(httpRequest, httpResponse, channelContext);

  }

  /**
   * 完整握手后
   */
  @Override
  public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) throws Exception {
    String path = httpRequest.getRequestLine().getPath();
    WebSocketRouter webSocketRouter = TioBootServer.me().getWebSocketRouter();
    if (webSocketRouter == null) {
      log.error("webSocketRoutes is null,please check");
      return;
    }

    IWebSocketHandler handler = webSocketRouter.find(path);
    handler.onAfterHandshaked(httpRequest, httpResponse, channelContext);
  }

  /**
   * 字节消息（binaryType = arraybuffer）过来后会走这个方法
   */
  @Override
  public Object onBytes(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    WebSocketRouter webSocketRouter = TioBootServer.me().getWebSocketRouter();
    if (webSocketRouter == null) {
      log.error("webSocketRoutes is null,please check");
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.onBytes(wsRequest, bytes, channelContext);
  }

  /**
   * 当客户端发close flag时，会走这个方法
   */
  @Override
  public Object onClose(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    WebSocketRouter webSocketRouter = TioBootServer.me().getWebSocketRouter();
    if (webSocketRouter == null) {
      log.error("webSocketRoutes is null,please check");
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.onClose(wsRequest, bytes, channelContext);
  }

  /*
   * 字符消息（binaryType = blob）过来后会走这个方法
   */
  @Override
  public Object onText(WsRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    WebSocketRouter webSocketRouter = TioBootServer.me().getWebSocketRouter();
    if (webSocketRouter == null) {
      log.error("webSocketRoutes is null,please check");
      return null;
    }

    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.onText(wsRequest, text, channelContext);
  }

}