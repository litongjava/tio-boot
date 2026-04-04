package nexus.io.tio.http.server.session;

import nexus.io.tio.http.common.Cookie;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.hutool.StrUtil;

public class HttpSessionUtils {

  public static String getSessionId(HttpRequest request) {
    String sessionId = request.getString(nexus.io.tio.http.common.HttpConfig.TIO_HTTP_SESSIONID);
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
