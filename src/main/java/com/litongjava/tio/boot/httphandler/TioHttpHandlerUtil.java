package com.litongjava.tio.boot.httphandler;

import java.lang.reflect.Method;

import org.tio.http.common.Cookie;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.RequestLine;
import org.tio.http.server.mvc.Routes;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.hutool.Validator;

/**
 * Created by litonglinux@qq.com on 11/9/2023_2:22 AM
 */
public class TioHttpHandlerUtil {
  public static Method getMethod(HttpConfig httpConfig, Routes routes, HttpRequest request, RequestLine requestLine) {
    Method method = null;
    String path = requestLine.path;
    if (routes != null) {
      method = routes.getMethodByPath(path, request);
      path = requestLine.path;
    }
    if (method == null) {
      if (StrUtil.isNotBlank(httpConfig.getWelcomeFile())) {
        if (StrUtil.endWith(path, "/")) {
          path = path + httpConfig.getWelcomeFile();
          requestLine.setPath(path);

          if (routes != null) {
            method = routes.getMethodByPath(path, request);
            path = requestLine.path;
          }
        }
      }
    }

    return method;
  }

  public static Cookie getSessionCookie(HttpRequest request, HttpConfig httpConfig) {
    Cookie sessionCookie = request.getCookie(httpConfig.getSessionCookieName());
    return sessionCookie;
  }

  public static String getSessionId(HttpRequest request) {
    String sessionId = request.getString(org.tio.http.common.HttpConfig.TIO_HTTP_SESSIONID);
    if (StrUtil.isNotBlank(sessionId)) {
      return sessionId;
    }

    Cookie cookie = TioHttpHandlerUtil.getSessionCookie(request, request.httpConfig);
    if (cookie != null) {
      return cookie.getValue();
    }

    return null;
  }
  
  public static String getDomain(HttpRequest request) {
    String domain = request.getDomain();

    boolean isip = Validator.isIpv4(domain);
    if (!isip) {
      String[] dms = StrUtil.split(domain, ".");
      if (dms.length > 2) {
        domain = "." + dms[dms.length - 2] + "." + dms[dms.length - 1];
      }
    }
    return domain;
  }

}
