package com.litongjava.tio.http.server.session;

import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.hutool.StrUtil;

public class HttpSessionUtils {

  public static String getSessionId(HttpRequest request) {
    String sessionId = request.getString(com.litongjava.tio.http.common.HttpConfig.TIO_HTTP_SESSIONID);
    if (StrUtil.isNotBlank(sessionId)) {
      return sessionId;
    }

    Cookie cookie = getSessionCookie(request, request.httpConfig);
    if (cookie != null) {
      return cookie.getValue();
    }

    return null;
  }
  
  public static Cookie getSessionCookie(HttpRequest request, HttpConfig httpConfig) {
    Cookie sessionCookie = request.getCookie(httpConfig.getSessionCookieName());
    return sessionCookie;
  }

}
