package com.litongjava.tio.http.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.litongjava.aio.Packet;
import com.litongjava.tio.client.ClientChannelContext;
import com.litongjava.tio.client.ClientTioConfig;
import com.litongjava.tio.client.TioClient;
import com.litongjava.tio.client.intf.ClientAioListener;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Node;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.client.handler.SimpleHttpClientAioHandler;
import com.litongjava.tio.http.client.packet.HttpRequestPacket;
import com.litongjava.tio.http.client.packet.HttpResponsePacket;
import com.litongjava.tio.proxy.ProxyInfo;

public class SimpleTioHttpClient {

  private final ProxyInfo proxyInfo; // 可为 null
  private final int connectTimeoutSec;

  public SimpleTioHttpClient(ProxyInfo proxyInfo, int connectTimeoutSec) {
    this.proxyInfo = proxyInfo;
    this.connectTimeoutSec = connectTimeoutSec <= 0 ? 5 : connectTimeoutSec;
  }

  public HttpResponsePacket get(String url, int timeoutSec) throws Exception {
    return request("GET", url, null, timeoutSec);
  }

  public HttpResponsePacket request(String method, String url, String body, int timeoutSec) throws Exception {
    URI uri = URI.create(url);
    String scheme = uri.getScheme();
    boolean ssl = "https".equalsIgnoreCase(scheme);

    String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1) port = ssl ? 443 : 80;

    String path = uri.getRawPath();
    if (path == null || path.isEmpty()) path = "/";
    if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) path += "?" + uri.getRawQuery();

    // 1) 构建 tio client
    SimpleHttpClientAioHandler handler = new SimpleHttpClientAioHandler();

    final Holder<HttpResponsePacket> holder = new Holder<>();
    CountDownLatch latch = new CountDownLatch(1);

    ClientAioListener listener = new ClientAioListener() {
      @Override
      public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
        // 这里不做事，发送在 request() 里统一控制
      }

      @Override
      public void onAfterDecoded(ChannelContext channelContext, Packet packet, int packetSize) throws Exception {
        if (packet instanceof HttpResponsePacket) {
          holder.value = (HttpResponsePacket) packet;
          latch.countDown();
          Tio.close(channelContext, "done");
        }
      }

      @Override public void onAfterReceivedBytes(ChannelContext channelContext, int receivedBytes) throws Exception {}
      @Override public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) throws Exception {}
      @Override public void onAfterHandled(ChannelContext channelContext, Packet packet, long cost) throws Exception {}
      @Override public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) throws Exception {}
    };

    ClientTioConfig cfg = new ClientTioConfig(handler, listener, null);
    cfg.setHeartbeatTimeout(0);

    if (ssl) {
      cfg.useSsl(); // 注意：代理场景下，SSL 真实握手会在 proxy CONNECT 完成后才进行（你框架内部会处理）
    }

    TioClient tioClient = new TioClient(cfg);

    // 2) 目标节点（注意：如果传了 proxyInfo，底层会先连 proxy，再 CONNECT 到 targetNode）
    Node targetNode = new Node(host, port);

    // 3) 连接（可能包含代理 CONNECT）
    ClientChannelContext cctx = tioClient.connect(targetNode, null, 0, connectTimeoutSec, this.proxyInfo);
    if (cctx == null) throw new RuntimeException("connect failed");

    // 4) HTTPS：等待握手完成后再发请求
    if (ssl) {
      waitSslHandshakeCompleted(cctx, timeoutSec <= 0 ? 10 : timeoutSec);
    }

    // 5) 发起 HTTP 请求
    byte[] reqBytes = buildHttpRequest(method, host, port, path, body);
    Tio.send(cctx, new HttpRequestPacket(reqBytes));

    // 6) 等响应
    boolean ok = latch.await(timeoutSec <= 0 ? 10 : timeoutSec, TimeUnit.SECONDS);
    if (!ok) {
      Tio.close(cctx, "timeout");
      throw new RuntimeException("http timeout");
    }
    return holder.value;
  }

  private static void waitSslHandshakeCompleted(ClientChannelContext cctx, int timeoutSec) throws InterruptedException {
    long deadline = System.currentTimeMillis() + Math.max(1, timeoutSec) * 1000L;

    while (true) {
      // 等 sslFacadeContext 被创建
      if (cctx.sslFacadeContext != null && cctx.sslFacadeContext.isHandshakeCompleted()) {
        return;
      }

      if (System.currentTimeMillis() > deadline) {
        // 这里直接抛异常更符合“握手失败/超时”的语义
        Tio.close(cctx, "ssl handshake timeout");
        throw new RuntimeException("ssl handshake timeout");
      }

      Thread.sleep(10);
    }
  }

  private static byte[] buildHttpRequest(String method, String host, int port, String path, String body) {
    StringBuilder sb = new StringBuilder();
    sb.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
    sb.append("Host: ").append(host).append(":").append(port).append("\r\n");
    sb.append("Connection: close\r\n");
    sb.append("User-Agent: tio-http-client/0.1\r\n");
    sb.append("Accept: */*\r\n");

    byte[] bodyBytes = null;
    if (body != null) {
      bodyBytes = body.getBytes(StandardCharsets.UTF_8);
      sb.append("Content-Type: text/plain; charset=utf-8\r\n");
      sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
    }
    sb.append("\r\n");

    byte[] head = sb.toString().getBytes(StandardCharsets.UTF_8);
    if (bodyBytes == null) return head;

    byte[] all = new byte[head.length + bodyBytes.length];
    System.arraycopy(head, 0, all, 0, head.length);
    System.arraycopy(bodyBytes, 0, all, head.length, bodyBytes.length);
    return all;
  }

  private static class Holder<T> { T value; }
}
