package com.litongjava.tio.websocket.client.config;

import java.util.function.Consumer;

import com.litongjava.tio.proxy.ProxyInfo;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.websocket.client.event.CloseEvent;
import com.litongjava.tio.websocket.client.event.ErrorEvent;
import com.litongjava.tio.websocket.client.event.MessageEvent;
import com.litongjava.tio.websocket.client.event.OpenEvent;

public class WebsocketClientConfig {
  private String charset = "UTF-8";
  private Consumer<CloseEvent> onClose;
  private Consumer<ErrorEvent> onError;
  private Consumer<MessageEvent> onMessage;
  private Consumer<OpenEvent> onOpen;
  private Consumer<Throwable> onThrows;

  private ProxyInfo proxyInfo;

  public WebsocketClientConfig() {
  }

  public WebsocketClientConfig(String charset) {
    this.charset = charset;
  }

  public WebsocketClientConfig(Consumer<MessageEvent> onMessage) {
    this.onMessage = onMessage;
  }

  public WebsocketClientConfig(Consumer<MessageEvent> onMessage, Consumer<ErrorEvent> onError) {
    this.onError = onError;
    this.onMessage = onMessage;
  }

  public WebsocketClientConfig(Consumer<MessageEvent> onMessage, Consumer<ErrorEvent> onError,
      Consumer<CloseEvent> onClose) {
    this.onClose = onClose;
    this.onError = onError;
    this.onMessage = onMessage;
  }

  public WebsocketClientConfig(Consumer<MessageEvent> onMessage, Consumer<ErrorEvent> onError,
      Consumer<CloseEvent> onClose, Consumer<Throwable> onThrows) {
    this.onError = onError;
    this.onMessage = onMessage;
    this.onClose = onClose;
    this.onThrows = onThrows;
  }

  public WebsocketClientConfig(Consumer<OpenEvent> onOpen, Consumer<MessageEvent> onMessage,
      Consumer<CloseEvent> onClose, Consumer<ErrorEvent> onError, Consumer<Throwable> onThrows) {
    this.charset = StrUtil.isBlank(charset) ? "UTF-8" : charset;
    this.onClose = onClose;
    this.onError = onError;
    this.onMessage = onMessage;
    this.onOpen = onOpen;
    this.onThrows = onThrows;
  }

  public Consumer<Throwable> getOnThrows() {
    return onThrows;
  }

  public void setOnThrows(Consumer<Throwable> onThrows) {
    this.onThrows = onThrows;
  }

  public String getCharset() {
    return charset;
  }

  // public void setCharset(String charset) {
  // this.charset = charset;
  // }

  public void setOnClose(Consumer<CloseEvent> onClose) {
    this.onClose = onClose;
  }

  public void setOnError(Consumer<ErrorEvent> onError) {
    this.onError = onError;
  }

  public void setOnMessage(Consumer<MessageEvent> onMessage) {
    this.onMessage = onMessage;
  }

  public void setOnOpen(Consumer<OpenEvent> onOpen) {
    this.onOpen = onOpen;
  }

  public Consumer<CloseEvent> getOnClose() {
    return onClose;
  }

  public Consumer<ErrorEvent> getOnError() {
    return onError;
  }

  public Consumer<MessageEvent> getOnMessage() {
    return onMessage;
  }

  public Consumer<OpenEvent> getOnOpen() {
    return onOpen;
  }

  public ProxyInfo getProxyInfo() {
    return proxyInfo;
  }

  public void setProxyInfo(ProxyInfo proxyInfo) {
    this.proxyInfo = proxyInfo;
  }
}
