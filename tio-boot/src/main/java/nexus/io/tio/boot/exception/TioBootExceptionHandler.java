package nexus.io.tio.boot.exception;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.websocket.common.WebSocketRequest;

public interface TioBootExceptionHandler {
  public Object handler(HttpRequest request, Throwable e);

  public Object wsTextHandler(WebSocketRequest webSokcetRequest, String text, ChannelContext channelContext, HttpRequest httpRequest, Throwable e);

  public Object wsBytesHandler(WebSocketRequest webSokcetRequest, byte[] bytes, ChannelContext channelContext, HttpRequest httpRequest, Throwable e);

}
