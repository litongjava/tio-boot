package nexus.io.tio.boot.utils;

import java.io.IOException;
import java.util.Locale;

import nexus.io.tio.http.common.HttpResponse;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Copies an OkHttp response into a tio HttpResponse.
 */
public class OkHttpResponseUtils {

  /**
   * Copies status, headers and the whole body.
   *
   * @param okHttpResponse upstream response
   * @param httpResponse   response to fill
   */
  public static void toTioHttpResponse(Response okHttpResponse, HttpResponse httpResponse) {
    if (okHttpResponse == null || httpResponse == null) {
      return;
    }
    copyStatusAndHeaders(okHttpResponse, httpResponse);

    ResponseBody body = okHttpResponse.body();
    if (body != null) {
      try {
        httpResponse.setBody(body.bytes());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Copies the status line and the end-to-end headers. Hop-by-hop headers,
   * Content-Length (recomputed by the encoder), Server and Date are skipped.
   *
   * @param okHttpResponse upstream response
   * @param httpResponse   response to fill
   */
  public static void copyStatusAndHeaders(Response okHttpResponse, HttpResponse httpResponse) {
    String message = okHttpResponse.message();
    httpResponse.setStatus(okHttpResponse.code(), message == null ? "" : message);

    Headers responseHeaders = okHttpResponse.headers();
    for (String name : responseHeaders.names()) {
      String lowerName = name.toLowerCase(Locale.ROOT);
      if (OkHttpRequestUtils.HOP_BY_HOP_HEADERS.contains(lowerName) || "server".equals(lowerName)
          || "date".equals(lowerName)) {
        continue;
      }
      httpResponse.addHeader(lowerName, responseHeaders.get(name));
    }
  }
}
