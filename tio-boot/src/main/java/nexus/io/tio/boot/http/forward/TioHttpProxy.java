package nexus.io.tio.boot.http.forward;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.http.common.sse.ChunkedPacket;
import nexus.io.tio.boot.utils.OkHttpRequestUtils;
import nexus.io.tio.boot.utils.OkHttpResponseUtils;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.common.RequestHeaderKey;
import nexus.io.tio.http.common.encoder.ChunkEncoder;
import nexus.io.tio.http.common.utils.HttpIpUtils;
import nexus.io.tio.utils.http.OkHttpClientPool;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;
import nexus.io.tio.utils.thread.TioThreadUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Reverse proxy built on OkHttp.
 * <p>
 * The request method, headers and raw body are forwarded unchanged. Responses
 * with a known length are buffered and returned as a normal HttpResponse.
 * Responses without a length (Server-Sent Events, chunked downloads) are
 * relayed chunk by chunk as they arrive, so streaming APIs such as LLM chat
 * work through the proxy.
 */
public class TioHttpProxy {
  private static final Logger log = LoggerFactory.getLogger(TioHttpProxy.class);

  private static final int STREAM_BUFFER_SIZE = 8192;

  public static void reverseProxy(String targetUrl, HttpRequest httpRequest, HttpResponse httpResponse) {
    reverseProxy(targetUrl, httpRequest, httpResponse, null);
  }

  /**
   * Forwards the request to {@code targetUrl + pathAndQuery}. When the upstream
   * cannot be reached the response is set to 502 Bad Gateway.
   *
   * @param targetUrl    scheme, host and optional path prefix
   * @param httpRequest  incoming request
   * @param httpResponse response to fill
   * @param callback     optional audit callback
   */
  public static void reverseProxy(String targetUrl, HttpRequest httpRequest, HttpResponse httpResponse,
      RequestProxyCallback callback) {
    String url = targetUrl + httpRequest.getRequestLine().getPathAndQuery();
    try {
      forward(OkHttpClientPool.getHttpClient(), url, httpRequest, httpResponse, callback);
    } catch (IOException e) {
      log.error("proxy {} failed: {}", url, e.getMessage());
      badGateway(httpResponse);
    }
  }

  /**
   * Forwards the request to an absolute url using the shared client.
   *
   * @param url          absolute target url including query string
   * @param httpRequest  incoming request
   * @param httpResponse response to fill
   * @return the response
   * @throws IOException when the upstream cannot be reached, so callers can try
   *                     another instance
   */
  public static HttpResponse forward(String url, HttpRequest httpRequest, HttpResponse httpResponse)
      throws IOException {
    return forward(OkHttpClientPool.getHttpClient(), url, httpRequest, httpResponse, null);
  }

  /**
   * Forwards the request to an absolute url.
   *
   * @param httpClient   client to use, e.g. one with a longer read timeout for
   *                     streaming APIs
   * @param url          absolute target url including query string
   * @param httpRequest  incoming request
   * @param httpResponse response to fill
   * @param callback     optional audit callback
   * @return the response; for streamed responses it is already sent and marked
   *         with {@code setSend(false)}
   * @throws IOException when the upstream cannot be reached
   */
  public static HttpResponse forward(OkHttpClient httpClient, String url, HttpRequest httpRequest,
      HttpResponse httpResponse, RequestProxyCallback callback) throws IOException {
    long id = SnowflakeIdUtils.id();
    String realIp = HttpIpUtils.getRealIp(httpRequest);
    if (callback != null) {
      TioThreadUtils.submit(() -> {
        try {
          callback.saveRequest(id, realIp, httpRequest);
        } catch (Exception e) {
          log.error("saveRequest failed", e);
        }
      });
    }

    addForwardedHeaders(httpRequest, realIp);
    Request okHttpRequest = OkHttpRequestUtils.buildOkHttpRequestWithUrl(url, httpRequest);

    long startTime = System.currentTimeMillis();
    Call call = httpClient.newCall(okHttpRequest);
    Response okHttpResponse = call.execute();
    try {
      long elapsed = System.currentTimeMillis() - startTime;
      if (isStreaming(httpRequest, okHttpResponse)) {
        stream(call, okHttpResponse, httpRequest, httpResponse);
        saveResponse(callback, id, elapsed, httpResponse, null);
        return httpResponse;
      }

      OkHttpResponseUtils.toTioHttpResponse(okHttpResponse, httpResponse);
      HeaderValue contentEncoding = httpResponse.getHeaders().get(HeaderName.Content_Encoding);
      if (contentEncoding != null) {
        // the body is already encoded by the upstream, send it as is
        httpResponse.setSkipGzipped(true);
      }
      saveResponse(callback, id, elapsed, httpResponse, httpResponse.getBody());
      return httpResponse;
    } finally {
      okHttpResponse.close();
    }
  }

  /**
   * Sets X-Forwarded-For, X-Forwarded-Proto and X-Forwarded-Host, keeping the
   * values set by a proxy in front of this server.
   */
  public static void addForwardedHeaders(HttpRequest httpRequest, String realIp) {
    String forwardedFor = httpRequest.getHeader(RequestHeaderKey.X_forwarded_For);
    String remoteIp = httpRequest.getClientIp();
    if (forwardedFor == null || forwardedFor.isEmpty()) {
      httpRequest.addHeader(RequestHeaderKey.X_forwarded_For, realIp);
    } else if (remoteIp != null && !forwardedFor.endsWith(remoteIp)) {
      httpRequest.addHeader(RequestHeaderKey.X_forwarded_For, forwardedFor + ", " + remoteIp);
    }
    if (httpRequest.getHeader(RequestHeaderKey.X_Forwarded_Proto) == null) {
      ChannelContext channelContext = httpRequest.channelContext;
      boolean ssl = channelContext != null && channelContext.sslFacadeContext != null;
      httpRequest.addHeader(RequestHeaderKey.X_Forwarded_Proto, ssl ? "https" : "http");
    }
    String host = httpRequest.getHeader("host");
    if (host != null && httpRequest.getHeader(RequestHeaderKey.X_Forwarded_Host) == null) {
      httpRequest.addHeader(RequestHeaderKey.X_Forwarded_Host, host);
    }
  }

  /**
   * A response is streamed when its length is unknown, which is the case for
   * Server-Sent Events and chunked downloads.
   */
  public static boolean isStreaming(HttpRequest httpRequest, Response okHttpResponse) {
    if ("HEAD".equals(okHttpResponse.request().method())) {
      return false;
    }
    ResponseBody body = okHttpResponse.body();
    if (body == null) {
      return false;
    }
    String contentType = okHttpResponse.header("Content-Type");
    if (contentType != null && contentType.toLowerCase().startsWith("text/event-stream")) {
      return true;
    }
    return body.contentLength() < 0 && okHttpResponse.code() != 204 && okHttpResponse.code() != 304;
  }

  /**
   * Sends the status line and headers, then relays each block of the upstream
   * body as an HTTP chunk. Stops and cancels the upstream call when the client
   * goes away.
   */
  private static void stream(Call call, Response okHttpResponse, HttpRequest httpRequest, HttpResponse httpResponse)
      throws IOException {
    OkHttpResponseUtils.copyStatusAndHeaders(okHttpResponse, httpResponse);
    httpResponse.addHeader(HeaderName.Transfer_Encoding, HeaderValue.from("chunked"));
    httpResponse.addHeader(HeaderName.X_Accel_Buffering, HeaderValue.from("no"));
    httpResponse.setSkipAddContentLength(true);
    httpResponse.setSkipGzipped(true);

    ChannelContext channelContext = httpRequest.channelContext;
    Tio.bSend(channelContext, httpResponse);
    httpResponse.setSend(false);

    BufferedSource source = okHttpResponse.body().source();
    byte[] buffer = new byte[STREAM_BUFFER_SIZE];
    boolean clientOpen = true;
    int read;
    while ((read = source.read(buffer)) != -1) {
      if (read == 0) {
        continue;
      }
      byte[] chunk = ChunkEncoder.encodeChunk(Arrays.copyOf(buffer, read));
      if (!Tio.bSend(channelContext, new ChunkedPacket(chunk))) {
        clientOpen = false;
        call.cancel();
        break;
      }
    }
    if (clientOpen) {
      Tio.bSend(channelContext, new ChunkedPacket(ChunkEncoder.encodeChunk(new byte[0])));
    }
  }

  private static void saveResponse(RequestProxyCallback callback, long id, long elapsed, HttpResponse httpResponse,
      byte[] body) {
    if (callback == null) {
      return;
    }
    HttpResponseStatus status = httpResponse.getStatus();
    Map<HeaderName, HeaderValue> headers = httpResponse.getHeaders();
    HeaderValue contentEncoding = headers.get(HeaderName.Content_Encoding);
    TioThreadUtils.submit(() -> {
      try {
        callback.saveResponse(id, elapsed, status.status, headers, contentEncoding, body);
      } catch (Exception e) {
        log.error("saveResponse failed", e);
      }
    });
  }

  private static void badGateway(HttpResponse httpResponse) {
    httpResponse.setStatus(HttpResponseStatus.C502);
    httpResponse.setBodyString("Bad Gateway");
  }
}
