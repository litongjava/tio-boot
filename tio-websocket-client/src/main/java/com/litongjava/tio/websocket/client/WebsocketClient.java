package com.litongjava.tio.websocket.client;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.litongjava.tio.client.ClientChannelContext;
import com.litongjava.tio.client.ClientTioConfig;
import com.litongjava.tio.client.TioClient;
import com.litongjava.tio.client.intf.ClientAioHandler;
import com.litongjava.tio.client.intf.ClientAioListener;
import com.litongjava.tio.websocket.client.config.WebsocketClientConfig;
import com.litongjava.tio.websocket.client.kit.UriKit;
import com.litongjava.tio.websocket.client.kit.WsPortUtils;

public class WebsocketClient {
  static ClientAioHandler tioClientHandler = new WebscoketClientAioHander();
  static ClientAioListener aioListener = new WebsocketClientAioListener();

  /**
   * To create a WsClient.
   *
   * @param uri The uri to connect
   * @return
   * @throws IOException
   */
  public static WebsocketClient create(String uri) throws Exception {
    return create(uri, (Map<String, String>) null);
  }

  /**
   * To create a WsClient.
   *
   * @param uri                   The uri to connect
   * @param additionalHttpHeaders Additional headers added to the http package
   *                              sent to the server during the handshake
   * @return
   * @throws IOException
   */
  public static WebsocketClient create(String uri, Map<String, String> additionalHttpHeaders) throws Exception {
    return new WebsocketClient(uri, additionalHttpHeaders);
  }

  /**
   * To create a WsClient.
   *
   * @param uri    The uri to connect
   * @param config The config of client. If you change the value later, you need
   *               to bear the possible consequences.
   * @return
   * @throws IOException
   */
  public static WebsocketClient create(String uri, WebsocketClientConfig config) throws Exception {
    return create(uri, null, config);
  }

  /**
   * To create a WsClient.
   *
   * @param uri                   The uri to connect
   * @param additionalHttpHeaders Additional headers added to the http package
   *                              sent to the server during the handshake
   * @param config                The config of client. If you change the value
   *                              later, you need to bear the possible
   *                              consequences.
   * @return
   * @throws IOException
   */
  public static WebsocketClient create(String uri, Map<String, String> additionalHttpHeaders,
      WebsocketClientConfig config) throws Exception {
    WebsocketClient client = new WebsocketClient(uri, additionalHttpHeaders);
    client.config = config;
    return client;
  }

  URI uri;
  String rawUri;
  TioClient tioClient;
  WebsocketClientConfig config = new WebsocketClientConfig();
  ClientChannelContext clientChannelContext;
  Map<String, String> additionalHttpHeaders;
  WebSocketImpl ws;
  ClientTioConfig clientTioConfig;
  int targetPort;

  WebsocketClient(String rawUri) throws Exception {
    this(rawUri, null);
  }

  WebsocketClient(String rawUri, Map<String, String> additionalHttpHeaders) throws Exception {
    rawUri = rawUri.trim();
    if (!rawUri.matches(
        "wss?\\://((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])|(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))(\\:[0-9]+)?(/.*)?(\\?.*)?")) {
      throw new Exception("Invalid uri of " + rawUri);
    }

    this.rawUri = rawUri;
    this.additionalHttpHeaders = additionalHttpHeaders;

    construct();
  }

  /**
   * connect to server
   *
   * @return WebSocket
   * @throws Exception
   */
  public synchronized WebSocket connect() throws Exception {
    ws.connect();
    return ws;
  }

  public void close() {
    if (ws != null) {
      ws.close();
      ws = null;
      clientChannelContext = null;
      clientTioConfig = null;
      tioClient = null;
    }
  }

  public WebSocket getWs() {
    return ws;
  }

  public WebsocketClientConfig getConfig() {
    return config;
  }

  public TioClient getTioClient() {
    return tioClient;
  }

  public ClientChannelContext getClientChannelContext() {
    return clientChannelContext;
  }

  public URI getUri() {
    return uri;
  }

  public String getRawUri() {
    return rawUri;
  }

  void construct() throws Exception {
    uri = UriKit.parseURI(rawUri);
    targetPort = WsPortUtils.getPort(uri);
//    try {
//      ReflectKit.setField(uri, "port", port);
//    } catch (Exception ex) {
//      log.error(ex.getMessage());
//    }

    clientTioConfig = new ClientTioConfig(tioClientHandler, aioListener, null);
    clientTioConfig.setHeartbeatTimeout(0);
    if (uri.getScheme().equals("ws")) {
      tioClient = new TioClient(clientTioConfig);
    } else {
      clientTioConfig.useSsl();
      tioClient = new TioClient(clientTioConfig);
    }
    ws = new WebSocketImpl(this, additionalHttpHeaders);
  }
}
