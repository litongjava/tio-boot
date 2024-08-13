package com.litongjava.tio.boot.http.forward;

import java.io.IOException;
import java.util.Map;

import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.http.common.ResponseHeaderKey;
import com.litongjava.tio.http.common.utils.HttpIpUtils;
import com.litongjava.tio.http.server.util.HttpServerRequestUtils;
import com.litongjava.tio.http.server.util.HttpServerResponseUtils;
import com.litongjava.tio.utils.http.OkHttpClientPool;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TioHttpProxy {
  public static void reverseProxy(String targetUrl, HttpRequest httpRequest, HttpResponse httpResponse, boolean save) {
    reverseProxy(targetUrl, httpRequest, httpResponse, save, null);
  }

  public static void reverseProxy(String targetUrl, HttpRequest httpRequest, HttpResponse httpResponse, boolean save,
      RequestProxyCallback callback) {
    // id

    long id = SnowflakeIdUtils.id();
    // remove host
    httpRequest.getHeaders().remove("host");
    // ip
    String realIp = HttpIpUtils.getRealIp(httpRequest);
    if (save && callback != null) {
      TioThreadUtils.getFixedThreadPool().submit(() -> {
        try {
          callback.saveRequest(id, realIp, httpRequest);
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }

    // set ip
    httpRequest.addHeader(RequestHeaderKey.X_forwarded_For, realIp);
    // build okhttp request
    Request okHttpReqeust = HttpServerRequestUtils.buildOkHttpRequest(targetUrl, httpRequest);

    OkHttpClient httpClient = OkHttpClientPool.getHttpClient();

    long startTime = System.currentTimeMillis();
    try (Response okHttpResponse = httpClient.newCall(okHttpReqeust).execute()) {
      long endTime = System.currentTimeMillis();

      HttpServerResponseUtils.fromOkHttp(okHttpResponse, httpResponse);

      // 使用ExecutorService异步执行任务
      HttpResponseStatus status = httpResponse.getStatus();

      Map<HeaderName, HeaderValue> headers = httpResponse.getHeaders();
      HeaderValue contentEncoding = headers.get(HeaderName.Content_Encoding);

      if (contentEncoding != null && HeaderValue.Content_Encoding.gzip.equals(contentEncoding)) {
        httpResponse.setHasGzipped(true);
      }

      byte[] body = httpResponse.getBody();
      if (save && callback != null) {
        TioThreadUtils.getFixedThreadPool().submit(() -> {
          try {
            callback.saveResponse(id, (endTime - startTime), status.status, headers, contentEncoding, body);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }

      httpResponse.removeHeaders(ResponseHeaderKey.Transfer_Encoding);
      httpResponse.removeHeaders(ResponseHeaderKey.Server);
      httpResponse.removeHeaders(ResponseHeaderKey.Date);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void reverseProxy(String remoteServerUrl, HttpRequest httpRequest, HttpResponse httpResponse) {
    reverseProxy(remoteServerUrl, httpRequest, httpResponse, false);
  }
}
