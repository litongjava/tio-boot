package com.litongjava.tio.boot.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.model.sys.SysConst;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.websocket.common.WebSocketRequest;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

/**
 * dispatcher
 * @author Tong Li
 *
 */
public class TioBootWebSocketDispatcher implements IWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(TioBootWebSocketDispatcher.class);
  
  private WebSocketRouter webSocketRouter = null;

  public TioBootWebSocketDispatcher() {

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

    if (handler != null) {
      HttpResponse handshake = null;
      try {
        handshake = handler.handshake(httpRequest, httpResponse, channelContext);
      } catch (Exception e) {
        httpErrorHandler(httpRequest, e);
      }
      return handshake;
    }
    HttpResponse response = new HttpResponse(httpRequest);
    response.setStatus(404);
    return response;

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
    if (handler != null) {
      try {
        handler.onAfterHandshaked(httpRequest, httpResponse, channelContext);
      } catch (Exception e) {
        httpErrorHandler(httpRequest, e);
      }
    }
  }

  /**
   * 字节消息（binaryType = arraybuffer）过来后会走这个方法
   */
  @Override
  public Object onBytes(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    HttpRequest httpRequest = wsSessionContext.getHandshakeRequest();
    String path = httpRequest.getRequestLine().path;

    if (webSocketRouter == null) {
      log.error("webSocket router is null,please check");
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    Object result = null;
    try {
      result = handler.onBytes(wsRequest, bytes, channelContext);
    } catch (Exception e) {
      StringBuilder sb = new StringBuilder();
      sb.append(SysConst.CRLF).append("Remote Address: ").append(httpRequest.getClientIp());
      sb.append(SysConst.CRLF).append("Request: ").append(httpRequest.getRequestLine().toString());
      log.error(sb.toString(), e);

      TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();

      if (exceptionHandler != null) {
        exceptionHandler.wsBytesHandler(wsRequest, bytes, channelContext, httpRequest, e);
      }
    }

    return result;
  }

  /**
   * 当客户端发close flag时，会走这个方法
   */
  @Override
  public Object onClose(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    HttpRequest httpRequest = wsSessionContext.getHandshakeRequest();
    String path = httpRequest.getRequestLine().path;

    if (webSocketRouter == null) {
      log.error("webSocket router is null,please check");
      return null;
    }
    IWebSocketHandler handler = webSocketRouter.find(path);
    Object packet = null;
    try {
      packet = handler.onClose(wsRequest, bytes, channelContext);
    } catch (Exception e) {
      StringBuilder sb = new StringBuilder();
      sb.append(SysConst.CRLF).append("Remote Address: ").append(httpRequest.getClientIp());
      sb.append(SysConst.CRLF).append("Request: ").append(httpRequest.getRequestLine().toString());
      log.error(sb.toString(), e);

      TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();

      if (exceptionHandler != null) {
        exceptionHandler.wsBytesHandler(wsRequest, bytes, channelContext, httpRequest, e);
      }
    }

    return packet;
  }

  /*
   * 字符消息（binaryType = blob）过来后会走这个方法
   */
  @Override
  public Object onText(WebSocketRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    HttpRequest httpRequest = wsSessionContext.getHandshakeRequest();
    String path = httpRequest.getRequestLine().path;

    if (webSocketRouter == null) {
      log.error("webSocket router is null,please check");
      return null;
    }

    IWebSocketHandler handler = webSocketRouter.find(path);
    Object packet = null;
    try {
      packet = handler.onText(wsRequest, text, channelContext);
    } catch (Exception e) {
      StringBuilder sb = new StringBuilder();
      sb.append(SysConst.CRLF).append("Remote Address: ").append(httpRequest.getClientIp());
      sb.append(SysConst.CRLF).append("Request: ").append(httpRequest.getRequestLine().toString());
      log.error(sb.toString(), e);

      TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();

      if (exceptionHandler != null) {
        exceptionHandler.wsTextHandler(wsRequest, text, channelContext, httpRequest, e);
      }
    }

    return packet;
  }

  public void httpErrorHandler(HttpRequest request, Throwable throwable) {
    StringBuilder sb = new StringBuilder();
    sb.append(SysConst.CRLF).append("Remote Address: ").append(request.getClientIp());
    sb.append(SysConst.CRLF).append("Request: ").append(request.getRequestLine().toString());
    log.error(sb.toString(), throwable);

    TioBootExceptionHandler exceptionHandler = TioBootServer.me().getExceptionHandler();

    if (exceptionHandler != null) {
      exceptionHandler.handler(request, throwable);
    }
  }
}