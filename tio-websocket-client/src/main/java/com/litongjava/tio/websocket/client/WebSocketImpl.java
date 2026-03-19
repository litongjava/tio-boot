package com.litongjava.tio.websocket.client;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.litongjava.aio.Packet;
import com.litongjava.tio.client.ClientChannelContext;
import com.litongjava.tio.consts.TioConst;
import com.litongjava.tio.core.Node;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpMethod;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.proxy.ProxyInfo;
import com.litongjava.tio.utils.base64.Base64Utils;
import com.litongjava.tio.utils.digest.Sha1Utils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.websocket.client.event.CloseEvent;
import com.litongjava.tio.websocket.client.event.ErrorEvent;
import com.litongjava.tio.websocket.client.event.MessageEvent;
import com.litongjava.tio.websocket.client.event.OpenEvent;
import com.litongjava.tio.websocket.client.httpclient.ClientHttpRequest;
import com.litongjava.tio.websocket.client.kit.ByteKit;
import com.litongjava.tio.websocket.client.kit.ObjKit;
import com.litongjava.tio.websocket.client.kit.TioKit;
import com.litongjava.tio.websocket.client.kit.WsPortUtils;
import com.litongjava.tio.websocket.common.Opcode;
import com.litongjava.tio.websocket.common.WebSocketPacket;
import com.litongjava.tio.websocket.common.WebSocketRequest;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class WebSocketImpl implements WebSocket {
  static final String packetPublisherKey = "__WS_PACKET_PUBLISHER__";
  static final String clientIntoCtxAttribute = "__WS_CLIENT__";
  private static final int maxBodyBytesLength = (int) (1024 * 1024 * 0.25); // 0.25 MB

  private volatile int readyState = WebSocket.CONNECTING;
  private String[] protocols = new String[] {};
  private WebsocketClient wsClient;
  private Map<String, String> additionalHttpHeaders;
  private ClientChannelContext ctx;
  private Subject<Packet> publisher = PublishSubject.<Packet>create().toSerialized();
  private String secWebsocketKey = null;

  // concurrent hash set
  private Set<Consumer<OpenEvent>> onOpenListenerSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private Set<Consumer<CloseEvent>> onCloseListenerSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private Set<Consumer<ErrorEvent>> onErrorListenerSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private Set<Consumer<Throwable>> onThrowsListenerSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private Subject<WebSocketPacket> sendWsPacketStream = PublishSubject.<WebSocketPacket>create().toSerialized();
  private Subject<Object> sendNotifier = PublishSubject.create().toSerialized();

  WebSocketImpl(WebsocketClient wsClient) {
    this(wsClient, null);
  }

  WebSocketImpl(WebsocketClient wsClient, Map<String, String> additionalHttpHeaders) {
    this.wsClient = wsClient;
    this.additionalHttpHeaders = additionalHttpHeaders;
    bindInitStreamObserver();
  }

  @SuppressWarnings("deprecation")
  @Override
  public synchronized void connect() throws Exception {
    CountDownLatch wg = new CountDownLatch(1);
    int i = 1;

    while (wsClient.clientChannelContext == null) {
      ProxyInfo proxyInfo = wsClient.config.getProxyInfo();

      String host = wsClient.uri.getHost();
      int port = WsPortUtils.getPort(wsClient.uri);
      Node target = new Node(host, port);

      wsClient.clientChannelContext = wsClient.tioClient.connect(target, proxyInfo);

      if (wsClient.clientChannelContext != null) {
        break;
      }

      wg.await(10L * i, TimeUnit.MILLISECONDS);
      i++;
    }

    ctx = wsClient.clientChannelContext;

    // Attach publisher before any handshake traffic is sent.
    ctx.setAttribute(packetPublisherKey, publisher);
    ctx.setAttribute(clientIntoCtxAttribute, wsClient);

    // Install a TLS handshake latch so upper-layer protocol can wait safely.
    CountDownLatch sslLatch = new CountDownLatch(1);
    ctx.setAttribute(TioConst.ATTR_SSL_HANDSHAKE_LATCH, sslLatch);

    WebSocketSessionContext session = new WebSocketSessionContext();
    ctx.set(session);

    // If this is a TLS connection, wait until the TLS handshake is completed.
    // This avoids sending HTTP Upgrade data too early.
    boolean isSsl = ctx.getTioConfig() != null && ctx.getTioConfig().isSsl();
    if (isSsl) {
      // Fast path: handshake already completed
      if (ctx.sslFacadeContext != null && ctx.sslFacadeContext.isHandshakeCompleted()) {
        // do nothing
      } else {
        boolean ok = sslLatch.await(15, TimeUnit.SECONDS);
        if (!ok) {
          close(1002, "ssl handshake timeout");
          return;
        }
      }
    }

    // Now it is safe to send WebSocket HTTP Upgrade request.
    handshake();
  }

  @Override
  public String getExtensions() {
    return null;
  }

  @Override
  public Runnable addOnClose(Consumer<CloseEvent> listener) {
    if (listener != null)
      onCloseListenerSet.add(listener);
    return () -> {
      if (listener != null)
        onCloseListenerSet.remove(listener);
    };
  }

  @Override
  public Runnable addOnError(Consumer<ErrorEvent> listener) {
    if (listener != null)
      onErrorListenerSet.add(listener);
    return () -> {
      if (listener != null)
        onErrorListenerSet.remove(listener);
    };
  }

  @Override
  public Runnable addOnMessage(Consumer<MessageEvent> listener) {
    Disposable disposable = getMessageStream().map(MessageEvent::new).subscribe(listener::accept);
    return disposable::dispose;
  }

  @Override
  public Runnable addOnOpen(Consumer<OpenEvent> listener) {
    if (listener != null)
      onOpenListenerSet.add(listener);
    return () -> {
      if (listener != null)
        onOpenListenerSet.remove(listener);
    };
  }

  @Override
  public Runnable addOnThrows(Consumer<Throwable> listener) {
    if (listener != null)
      onThrowsListenerSet.add(listener);
    return () -> {
      if (listener != null)
        onThrowsListenerSet.remove(listener);
    };
  }

  private void onOpen() {
    OpenEvent openEvent = new OpenEvent();
    Consumer<OpenEvent> onOpen = wsClient.config.getOnOpen();
    if (onOpen != null) {
      onOpen.accept(openEvent);
    }
    onOpenListenerSet.forEach(it -> it.accept(openEvent));
    sendNotifier.onNext(true);
  }

  private void onClose(int code, String reason) {
    sendWsPacketStream.onComplete();
    Consumer<CloseEvent> onClose = wsClient.config.getOnClose();
    if (onClose != null) {
      onClose.accept(new CloseEvent(code, reason, ctx.isRemoved));
    }
    onCloseListenerSet.forEach(it -> it.accept(new CloseEvent(code, reason, ctx.isRemoved)));
  }

  @SuppressWarnings("unused")
  private void onError(String msg) {
    sendWsPacketStream.onComplete();
    ErrorEvent errorEvent = new ErrorEvent(msg);
    Consumer<ErrorEvent> onError = wsClient.config.getOnError();
    if (onError != null) {
      onError.accept(errorEvent);
    }
    onErrorListenerSet.forEach(it -> it.accept(errorEvent));
  }

  private void onThrows(Throwable e) {
    Consumer<Throwable> onThrows = wsClient.config.getOnThrows();
    if (onThrows != null) {
      onThrows.accept(e);
    }
    onThrowsListenerSet.forEach(it -> it.accept(e));
  }

  @Override
  public String getProtocol() {
    StringBuilder p = new StringBuilder();
    int i = 0;
    for (String proto : protocols) {
      p.append(proto);
      if (i != 0 && i != protocols.length - 1) {
        p.append(",");
      }
      i++;
    }
    return p.toString();
  }

  @Override
  public int getReadyState() {
    return readyState;
  }

  @Override
  public String getUrl() {
    return wsClient.rawUri;
  }

  @Override
  public synchronized void close(int code, String reason) {
    if (readyState == WebSocket.CLOSED)
      return;
    if (readyState != WebSocket.CLOSING) {
      readyState = WebSocket.CLOSING;
      WebSocketPacket close = new WebSocketPacket();
      close.setWsOpcode(Opcode.CLOSE);
      if (StrUtil.isBlank(reason))
        reason = "";
      try {
        byte[] reasonBytes = reason.getBytes("UTF-8");
        short c = (short) code;
        ByteBuffer body = ByteBuffer.allocate(2 + reasonBytes.length);
        body.putShort(c);
        body.put(reasonBytes);
        close.setBody(body.array());
        close.setWsBodyLength(close.getBody().length);
      } catch (UnsupportedEncodingException e) {
      }
      Tio.send(ctx, close);
      String finalReason = reason;
      Observable.timer(1, TimeUnit.SECONDS).subscribe(i -> {
        clear(code, finalReason);
      });
    } else {
      clear(code, reason);
    }
  }

  synchronized void clear(int code, String reason) {
    if (readyState == WebSocket.CLOSED)
      return;
    readyState = WebSocket.CLOSED;
    publisher.onComplete();
    onClose(code, reason);
    try {
      wsClient.tioClient.stop();
    } catch (Exception e) {
    }
  }

  @Override
  public void send(String data) {
    send(WebSocketRequest.fromText(data, wsClient.config.getCharset()));
  }

  @Override
  public void send(WebSocketPacket packet) {
    sendWsPacketStream.onNext(packet);
    if (readyState == WebSocket.OPEN)
      sendNotifier.onNext(true);
  }

  private synchronized void sendImmediately(WebSocketPacket packet) {
    byte[] wsBody = packet.getBody();
    byte[][] wsBodies = packet.getBodys();
    int wsBodyLength = 0;
    if (wsBody != null) {
      wsBodyLength += wsBody.length;
    } else if (wsBodies != null) {
      for (byte[] bs : wsBodies) {
        wsBodyLength += bs.length;
      }
    }
    ByteBuffer bodyBuf = null;
    if (wsBody != null && wsBody.length > 0) {
      bodyBuf = ByteBuffer.wrap(wsBody);
    } else if (wsBodies != null) {
      bodyBuf = ByteBuffer.allocate(wsBodyLength);
      for (byte[] bs : wsBodies) {
        bodyBuf.put(bs);
      }
    }
    if (bodyBuf == null || wsBodyLength == 0) {
      Tio.send(ctx, packet);
    } else {
      if (wsBodyLength <= maxBodyBytesLength) {
        packet.setBody(bodyBuf.array());
        packet.setBodys(null);
        Tio.send(ctx, packet);
      } else {
        byte[][] parts = ByteKit.split(bodyBuf.array(), maxBodyBytesLength);
        for (int i = 0; i < parts.length; i++) {
          byte[] body = parts[i];
          WebSocketPacket sentPacket = cloneWsPacket(packet);
          sentPacket.setBodys(null);
          sentPacket.setBody(body);
          sentPacket.setWsBodyLength(body.length);
          if (i == 0) {
            sentPacket.setWsEof(false);
          } else if (i < parts.length - 1) {
            sentPacket.setWsEof(false);
            sentPacket.setWsOpcode(Opcode.NOT_FIN);
          } else {
            sentPacket.setWsEof(true);
            sentPacket.setWsOpcode(Opcode.NOT_FIN);
          }
          TioKit.bSend(ctx, sentPacket, 60, TimeUnit.SECONDS);
        }
      }
    }
  }

  @Override
  public Observable<WebSocketPacket> getMessageStream() {
    return getWsPacketStream()
        .filter(p -> p.getWsOpcode().equals(Opcode.BINARY) || p.getWsOpcode().equals(Opcode.TEXT));
  }

  private Observable<WebSocketPacket> getWsPacketStream() {
    return publisher.filter(p -> p instanceof WebSocketPacket).map(p -> (WebSocketPacket) p);
  }

  private void handshake() {
    readyState = WebSocket.CONNECTING;

    ClientChannelContext ctx = wsClient.getClientChannelContext();
    WebSocketSessionContext session = (WebSocketSessionContext) ctx.get();

    session.setHandshaked(false);

    String path = wsClient.uri.getPath();
    if (StrUtil.isBlank(path)) {
      path = "/";
    }
    ClientHttpRequest httpRequest = new ClientHttpRequest(HttpMethod.GET, path, wsClient.uri.getRawQuery());
    Map<String, String> headers = new HashMap<>();
    if (additionalHttpHeaders != null) {
      headers.putAll(additionalHttpHeaders);
    }

    int port = wsClient.targetPort;
    headers.put("Host", wsClient.uri.getHost() + ":" + port);
    headers.put("Upgrade", "websocket");
    headers.put("Connection", "Upgrade");
    headers.put("Sec-WebSocket-Key", getSecWebsocketKey());
    headers.put("Sec-WebSocket-Version", "13");
    httpRequest.setHeaders(headers);

    session.setHandshakeRequest(httpRequest);

    ObjKit.Box<Disposable> disposableBox = ObjKit.box(null);

    disposableBox.value = publisher.filter(packet -> !session.isHandshaked()).subscribe(packet -> {
      if (packet instanceof HttpResponse) {
        HttpResponse resp = (HttpResponse) packet;

        if (resp.getStatus() == HttpResponseStatus.C101) {
          HeaderValue upgrade = resp.getHeader(HeaderName.Upgrade);
          if (upgrade == null || !upgrade.value.toLowerCase().equals("websocket")) {
            close(1002, "no upgrade or upgrade invalid");
            return;
          }
          HeaderValue connection = resp.getHeader(HeaderName.Connection);
          if (connection == null || !connection.value.toLowerCase().equals("upgrade")) {
            close(1002, "no connection or connection invalid");
            return;
          }
          HeaderValue secWebsocketAccept = resp.getHeader(HeaderName.Sec_WebSocket_Accept);
          if (secWebsocketAccept == null || !verifySecWebsocketAccept(secWebsocketAccept.value)) {
            close(1002, "no Sec_WebSocket_Accept or Sec_WebSocket_Accept invalid");
            return;
          }
          // TODO: Sec-WebSocket-Extensions, Sec-WebSocket-Protocol
          readyState = WebSocket.OPEN;
          session.setHandshaked(true);
          onOpen();
        } else {
          // TODO: support other http code
          close(1002, "not support http code: " + resp.getStatus().status);
          return;
        }

        disposableBox.value.dispose();
      }
    });

    Tio.send(ctx, httpRequest);
  }

  private String getSecWebsocketKey() {
    if (secWebsocketKey == null) {
      byte[] bytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        bytes[i] = (byte) (Math.random() * 256);
      }
      secWebsocketKey = Base64Utils.encodeToString(bytes);
    }
    return secWebsocketKey;
  }

  private boolean verifySecWebsocketAccept(String secWebsocketAccept) {
    byte[] digest = Sha1Utils.digest(secWebsocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
    return Base64Utils.encodeToString(digest).equals(secWebsocketAccept);
  }

  private void bindInitStreamObserver() {
    sendWsPacketStream.buffer(sendNotifier) // Is it need back pressure control?
        .subscribe(packets ->
        //
        packets.forEach(this::sendImmediately), this::onThrows, sendNotifier::onComplete);

    getMessageStream().subscribe(p -> {
      Consumer<MessageEvent> onMessage = wsClient.config.getOnMessage();
      if (onMessage != null) {
        onMessage.accept(new MessageEvent(p));
      }
    }, this::onThrows);
    getWsPacketStream().filter(p -> p.getWsOpcode().equals(Opcode.CLOSE)).subscribe(packet -> {
      if (readyState == WebSocket.CLOSED)
        return;
      byte[] body = packet.getBody();
      short code = 1000;
      String reason = "";
      if (body != null && body.length >= 2) {
        ByteBuffer bodyBuf = ByteBuffer.wrap(body);
        code = bodyBuf.getShort();
        byte[] reasonBytes = new byte[body.length - 2];
        bodyBuf.get(reasonBytes, 0, reasonBytes.length);
        reason = new String(reasonBytes, "UTF-8");
      }
      if (readyState == WebSocket.CLOSING) {
        clear(code, reason);
      } else {
        readyState = WebSocket.CLOSING;
        packet.setBody(ByteBuffer.allocate(2).putShort(code).array());
        Tio.send(ctx, packet);
        close(code, reason);
      }
    });
    getWsPacketStream().filter(p -> p.getWsOpcode().equals(Opcode.PING)).subscribe(packet -> {
      WebSocketPacket pong = new WebSocketPacket();
      pong.setWsOpcode(Opcode.PONG);
      pong.setWsEof(true);
      Tio.send(ctx, pong);
    });
  }

  private static WebSocketPacket cloneWsPacket(WebSocketPacket p) {
    WebSocketPacket packet = new WebSocketPacket();
    packet.setHandShake(p.isHandShake());
    packet.setBody(p.getBody());
    packet.setBodys(p.getBodys());
    packet.setWsEof(p.isWsEof());
    packet.setWsOpcode(p.getWsOpcode());
    packet.setWsHasMask(p.isWsHasMask());
    packet.setWsBodyLength(p.getWsBodyLength());
    packet.setWsMask(p.getWsMask());
    packet.setWsBodyText(p.getWsBodyText());
    return packet;
  }
}
