package com.litongjava.tio.boot.websocket;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.websocket.common.WebSocketRequest;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * dispather
 * @author Tong Li
 *
 */
@Slf4j
public class TioBootWebSocketDispather implements IWebSocketHandler {

  public WebSocketRouter webSocketRouter = null;

  public TioBootWebSocketDispather() {
  }

  public void setWebSocketRouter(WebSocketRouter webSocketRouter) {
    this.webSocketRouter = webSocketRouter;
  }

  /**
   * 握手时走这个方法，业务可以在这里获取cookie，request参数等
   */
  @Override
  public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) throws Exception {
    String path = httpRequest.getRequestLine().getPath();

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
  public Object onBytes(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    if (webSocketRouter == null) {
      log.error("webSocket router is null,please check");
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.onBytes(wsRequest, bytes, channelContext);
  }

  /**
   * 当客户端发close flag时，会走这个方法
   */
  @Override
  public Object onClose(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    if (webSocketRouter == null) {
      log.error("webSocket router is null,please check");
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.onClose(wsRequest, bytes, channelContext);
  }

  /*
   * 字符消息（binaryType = blob）过来后会走这个方法
   */
  @Override
  public Object onText(WebSocketRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    if (webSocketRouter == null) {
      log.error("webSocket router is null,please check");
      return null;
    }

    IWebSocketHandler handler = webSocketRouter.find(path);
    return handler.onText(wsRequest, text, channelContext);
  }

}