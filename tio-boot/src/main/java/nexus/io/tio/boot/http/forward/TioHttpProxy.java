package nexus.io.tio.boot.http.forward;

import java.io.IOException;
import java.util.Map;

import com.litongjava.tio.utils.http.OkHttpClientPool;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;

import nexus.io.tio.boot.utils.OkHttpRequestUtils;
import nexus.io.tio.boot.utils.OkHttpResponseUtils;
import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.common.RequestHeaderKey;
import nexus.io.tio.http.common.ResponseHeaderKey;
import nexus.io.tio.http.common.utils.HttpIpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TioHttpProxy {
  public static void reverseProxy(String targetUrl, HttpRequest httpRequest, HttpResponse httpResponse) {
    reverseProxy(targetUrl, httpRequest, httpResponse, null);
  }

  public static void reverseProxy(String targetUrl, HttpRequest httpRequest, HttpResponse httpResponse, RequestProxyCallback callback) {
    // id

    long id = SnowflakeIdUtils.id();
    // remove host
    httpRequest.getHeaders().remove("host");
    // ip
    String realIp = HttpIpUtils.getRealIp(httpRequest);
    if (callback != null) {
      TioThreadUtils.submit(() -> {
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
    Request okHttpReqeust = OkHttpRequestUtils.buildOkHttpRequest(targetUrl, httpRequest);

    OkHttpClient httpClient = OkHttpClientPool.getHttpClient();

    long startTime = System.currentTimeMillis();
    try (Response okHttpResponse = httpClient.newCall(okHttpReqeust).execute()) {
      long endTime = System.currentTimeMillis();

      OkHttpResponseUtils.toTioHttpResponse(okHttpResponse, httpResponse);

      HttpResponseStatus status = httpResponse.getStatus();

      Map<HeaderName, HeaderValue> headers = httpResponse.getHeaders();
      HeaderValue contentEncoding = headers.get(HeaderName.Content_Encoding);

      if (contentEncoding != null && HeaderValue.Content_Encoding.gzip.equals(contentEncoding)) {
        httpResponse.setSkipGzipped(true);
      }

      byte[] body = httpResponse.getBody();
      if (callback != null) {
        // 使用ExecutorService异步执行任务
        TioThreadUtils.submit(() -> {
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
}
