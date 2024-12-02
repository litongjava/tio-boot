package com.litongjava.tio.boot.exception;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.websocket.common.WebSocketRequest;

public interface TioBootExceptionHandler {
  public Object handler(HttpRequest request, Throwable e);

  public Object wsTextHandler(WebSocketRequest webSokcetRequest, String text, ChannelContext channelContext, HttpRequest httpRequest, Throwable e);

  public Object wsBytesHandler(WebSocketRequest webSokcetRequest, byte[] bytes, ChannelContext channelContext, HttpRequest httpRequest, Throwable e);

}
