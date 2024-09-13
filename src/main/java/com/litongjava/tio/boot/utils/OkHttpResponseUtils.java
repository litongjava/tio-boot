package com.litongjava.tio.boot.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.litongjava.tio.http.common.HttpResponse;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpResponseUtils {

  public static void toTioHttpResponse(Response okHttpResponse, HttpResponse httpResponse) {
    if (okHttpResponse == null || httpResponse == null) {
      return;
    }

    // Setting the status code
    httpResponse.setStatus(okHttpResponse.code(), okHttpResponse.message());

    // Setting headers
    Headers responseHeaders = okHttpResponse.headers();
    for (String name : responseHeaders.names()) {
      httpResponse.addHeader(name.toLowerCase(), responseHeaders.get(name));
    }

    // Setting the response body
    ResponseBody body = okHttpResponse.body();
    if (body != null) {
      try (InputStream byteStream = body.byteStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
        // Extract bytes from response body
        int read;
        byte[] buffer = new byte[1024 * 2];
        // Assuming you want to send the whole body in the httpResponse
        while ((read = byteStream.read(buffer)) != -1) {
          baos.write(buffer, 0, read);
        }
        httpResponse.setBody(baos.toByteArray());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
  }
}
