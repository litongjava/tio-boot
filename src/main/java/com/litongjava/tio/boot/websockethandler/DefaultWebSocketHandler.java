package com.litongjava.tio.boot.websockethandler;

import org.tio.core.ChannelContext;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.websocket.common.WsRequest;
import org.tio.websocket.common.WsSessionContext;
import org.tio.websocket.server.handler.IWsMsgHandler;

import com.litongjava.jfinal.aop.Aop;

/**
 * dispather
 * @author Tong Li
 *
 */
public class DefaultWebSocketHandler implements IWsMsgHandler {

  /**
   * 握手时走这个方法，业务可以在这里获取cookie，request参数等
   */
  @Override
  public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext)
      throws Exception {
    String path = httpRequest.getRequestLine().getPath();
    WebSocketRoutes webSocketRoutes = Aop.get(WebSocketRoutes.class);
    IWsMsgHandler handler = Aop.get(webSocketRoutes.get(path));
    return handler.handshake(httpRequest, httpResponse, channelContext);
  }

  /**
   * 完整握手后
   */
  @Override
  public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext)
      throws Exception {
    String path = httpRequest.getRequestLine().getPath();
    WebSocketRoutes webSocketRoutes = Aop.get(WebSocketRoutes.class);
    IWsMsgHandler handler = Aop.get(webSocketRoutes.get(path));
    handler.onAfterHandshaked(httpRequest, httpResponse, channelContext);
  }

  /**
   * 字节消息（binaryType = arraybuffer）过来后会走这个方法
   */
  @Override
  public Object onBytes(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    WebSocketRoutes webSocketRoutes = Aop.get(WebSocketRoutes.class);
    IWsMsgHandler handler = Aop.get(webSocketRoutes.get(path));
    return handler.onBytes(wsRequest, bytes, channelContext);
  }

  /**
   * 当客户端发close flag时，会走这个方法
   */
  @Override
  public Object onClose(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    WebSocketRoutes webSocketRoutes = Aop.get(WebSocketRoutes.class);
    IWsMsgHandler handler = Aop.get(webSocketRoutes.get(path));
    return handler.onClose(wsRequest, bytes, channelContext);
  }

  /*
   * 字符消息（binaryType = blob）过来后会走这个方法
   */
  @Override
  public Object onText(WsRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
    WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
    String path = wsSessionContext.getHandshakeRequest().getRequestLine().path;

    WebSocketRoutes webSocketRoutes = Aop.get(WebSocketRoutes.class);
    IWsMsgHandler handler = Aop.get(webSocketRoutes.get(path));
    return handler.onText(wsRequest, text, channelContext);
  }

}