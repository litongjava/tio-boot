package nexus.io.tio.http.common.utils;

import java.util.Locale;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.utils.hutool.ZipUtil;

/**
 * @author tanyaowu
 * 2017年8月18日 下午5:47:00
 */
public class HttpGzipUtils {
  
  /**
   * gzip
   * @param request
   * @param response
   */
  public static void gzip(HttpRequest request, HttpResponse response) {
    if (response == null) {
      return;
    }

    // 已经gzip过了，就不必再压缩了
    if (response.isSkipGzipped()) {
      return;
    }

    HeaderValue ct = response.getContentType();

    String mime = ct != null ? ct.getValue().toLowerCase(Locale.ROOT) : "";
    boolean isBinary = mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/");
    if (isBinary) {
      return;
    }

    byte[] body = response.getBody();
    if (body == null || body.length < 300) {
      return;
    }
    // Both compressed and identity responses depend on Accept-Encoding.
    response.mergeVary("Accept-Encoding");
    if (request != null && request.getIsSupportGzip()) {
      justGzip(response);
    }
  }

  /**
   * 
   * @param response
   * @author tanyaowu
   */
  public static void gzip(HttpResponse response) {
    if (response != null) {
      gzip(response.getHttpRequest(), response);
    }
  }

  public static void justGzip(HttpResponse response) {
    byte[] bs = response.getBody();
    if (bs != null && bs.length >= 300) {
      byte[] bs2 = ZipUtil.gzip(bs);
      if (bs2.length < bs.length) {
        response.setBody(bs2);
        response.setSkipGzipped(true);
        response.addHeader(HeaderName.Content_Encoding, HeaderValue.Content_Encoding.gzip);
      }
    }
  }
}
