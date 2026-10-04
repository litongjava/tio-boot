package nexus.io.tio.http.server;

import java.nio.ByteBuffer;
import nexus.io.tio.core.pool.EncodedBuffer;

import nexus.io.aio.Packet;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.core.TioConfig;
import nexus.io.tio.core.exception.TioDecodeException;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpRequestDecoder;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseEncoder;
import nexus.io.tio.http.common.handler.ITioHttpRequestHandler;
import nexus.io.tio.server.intf.ServerAioHandler;

/**
 *
 * @author tanyaowu
 *
 */
public class HttpServerAioHandler implements ServerAioHandler {
  public static final String REQUEST_KEY = "tio_request_key";
  private static final String HTTP_CLOSING = HttpServerAioHandler.class.getName() + ".httpClosing";
  protected HttpConfig httpConfig;
  private ITioHttpRequestHandler requestHandler;

  /**
   * @author tanyaowu 2016年11月18日 上午9:13:15
   *
   */
  public HttpServerAioHandler(HttpConfig httpConfig, ITioHttpRequestHandler requestHandler) {
    this.httpConfig = httpConfig;
    this.requestHandler = requestHandler;
  }

  @Override
  public HttpRequest decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
    HttpRequest request = HttpRequestDecoder.decode(buffer, limit, position, readableLength, channelContext, httpConfig);
    if (request != null) {
      channelContext.setAttribute(REQUEST_KEY, request);
    }
    return request;
  }

  @Override
  public EncodedBuffer encodeBuffer(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    if (getClass() != HttpServerAioHandler.class) return ServerAioHandler.super.encodeBuffer(packet, tioConfig, channelContext);
    return HttpResponseEncoder.encodeBuffer((HttpResponse) packet, tioConfig, channelContext);
  }

  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    HttpResponse httpResponse = (HttpResponse) packet;
    return HttpResponseEncoder.encode(httpResponse, tioConfig, channelContext);
  }

  /**
   * @return the httpConfig
   */
  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  @Override
  public void handler(Packet packet, ChannelContext channelContext) throws Exception {
    if (Boolean.TRUE.equals(channelContext.getAttribute(HTTP_CLOSING))) {
      return;
    }
    HttpRequest request = (HttpRequest) packet;

    String ip = request.getClientIp();

    if (channelContext.tioConfig.ipBlacklist != null) {
      if (channelContext.tioConfig.ipBlacklist.isInBlacklist(ip)) {
        HttpResponse httpResponse = request.httpConfig.getRespForBlackIp();
        if (httpResponse != null) {
          prepareResponse(httpResponse, channelContext);
          if (httpResponse.isBlockSend()) {
            Tio.bSend(channelContext, httpResponse);
          } else {
            Tio.send(channelContext, httpResponse);
          }
          return;
        } else {
          Tio.remove(channelContext, ip + "in the blacklist");
          return;
        }
      }
    }

    HttpResponse httpResponse = requestHandler.handler(request);
    if (httpResponse != null && httpResponse.isSend()) {
      prepareResponse(httpResponse, channelContext);
      Tio.send(channelContext, httpResponse);
    }
  }

  private void prepareResponse(HttpResponse response, ChannelContext channelContext) {
    response.prepareConnection();
    if (!response.isKeepConnection()) {
      // Do not execute later pipelined requests while the last response is pending.
      // isWaitingClose cannot be used here: it would discard that response's bytes.
      channelContext.setAttribute(HTTP_CLOSING, Boolean.TRUE);
    }
  }

  /**
   * @param httpConfig the httpConfig to set
   */
  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

}
