package com.litongjava.tio.http.common.utils;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.utils.hutool.ZipUtil;

/**
 * @author tanyaowu
 * 2017年8月18日 下午5:47:00
 */
public class HttpGzipUtils {
  private static final Logger log = LoggerFactory.getLogger(HttpGzipUtils.class);
  
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

    if (request != null && request.getIsSupportGzip()) {
      justGzip(response);
    } else {
      if (request != null) {
        log.warn("not support gzip:{}, {}", request.getClientIp(), request.getHeader(RequestHeaderKey.User_Agent));
      }
    }
  }

  /**
   * 
   * @param response
   * @author tanyaowu
   */
  public static void gzip(HttpResponse response) {
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

    justGzip(response);
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
