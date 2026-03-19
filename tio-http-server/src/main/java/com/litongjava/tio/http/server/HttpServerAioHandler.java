package com.litongjava.tio.http.server;

import java.nio.ByteBuffer;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpRequestDecoder;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseEncoder;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.server.intf.ServerAioHandler;

/**
 *
 * @author tanyaowu
 *
 */
public class HttpServerAioHandler implements ServerAioHandler {
  public static final String REQUEST_KEY = "tio_request_key";
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
    HttpRequest request = (HttpRequest) packet;

    String ip = request.getClientIp();

    if (channelContext.tioConfig.ipBlacklist != null) {
      if (channelContext.tioConfig.ipBlacklist.isInBlacklist(ip)) {
        HttpResponse httpResponse = request.httpConfig.getRespForBlackIp();
        if (httpResponse != null) {
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
      Tio.send(channelContext, httpResponse);
    }
  }

  /**
   * @param httpConfig the httpConfig to set
   */
  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

}
