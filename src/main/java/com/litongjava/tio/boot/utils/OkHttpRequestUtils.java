package com.litongjava.tio.boot.utils;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.http.common.HttpMethod;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.common.UploadFile;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;

public class OkHttpRequestUtils {
  private static final Logger log = LoggerFactory.getLogger(OkHttpRequestUtils.class);
  
  public static Request buildOkHttpRequest(String prefix, HttpRequest httpRequest) {
    RequestLine requestLine = httpRequest.getRequestLine();
    HttpMethod requestMethod = requestLine.getMethod();
    String pathAndQuery = requestLine.getPathAndQuery();
    String targetUrl = prefix + pathAndQuery;

    Builder requestBuilder = new Request.Builder();
    requestBuilder.url(targetUrl);

    // 打印请求头信息
    Map<String, String> requestHeaders = httpRequest.getHeaders();
    Headers headers = Headers.of(requestHeaders);
    requestBuilder.headers(headers);

    // 设置请求体
    if (HttpMethod.GET.equals(requestMethod)) {
      requestBuilder.get();
    } else if (HttpMethod.POST.equals(requestMethod)) {
      requestBuilder.post(buildRequestBody(httpRequest));

    } else if (HttpMethod.PUT.equals(requestMethod)) {
      requestBuilder.put(buildRequestBody(httpRequest));

    } else if (HttpMethod.DELETE.equals(requestMethod)) {
      requestBuilder.delete(buildRequestBody(httpRequest));
    } else {
      log.error("unknows method:{}", requestMethod);
    }

    return requestBuilder.build();
  }

  /**
   * build ok http request body object
   * 
   * @param httpRequest
   * @return
   */
  @SuppressWarnings("deprecation")
  private static RequestBody buildRequestBody(HttpRequest httpRequest) {
    String contentType = httpRequest.getContentType();
    if (contentType != null) {
      if (contentType.startsWith("application/json")) {
        MediaType mediaType = MediaType.parse(contentType);
        return RequestBody.create(mediaType, httpRequest.getBodyString());

      } else if (contentType.startsWith("application/x-www-form-urlencoded")) {
        FormBody.Builder builder = new FormBody.Builder();
        Map<String, Object[]> params = httpRequest.getParams();
        for (Map.Entry<String, Object[]> e : params.entrySet()) {
          // 添加参数
          builder.add(e.getKey(), (String) e.getValue()[0]);
        }
        return builder.build();

      } else if (contentType.startsWith("multipart/form-data")) {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        Map<String, Object[]> params = httpRequest.getParams();
        for (Map.Entry<String, Object[]> e : params.entrySet()) {
          Object value = e.getValue()[0];
          // 添加参数
          if (value instanceof String) {
            builder.addFormDataPart(e.getKey(), (String) value);
          } else {
            UploadFile uploadFile = httpRequest.getUploadFile(e.getKey());
            RequestBody fileBody = RequestBody.create(uploadFile.getData());
            builder.addFormDataPart(e.getKey(), uploadFile.getName(), fileBody);
          }
        }
        return builder.build();
      } else {
        MediaType mediaType = MediaType.parse(contentType);
        return RequestBody.create(mediaType, httpRequest.getBodyString());
      }

    } else {
      MediaType mediaType = MediaType.parse("text/plain");
      return RequestBody.create(mediaType, "");
    }
  }
}
