package com.litongjava.tio.boot.handler;

import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.RequestLine;
import org.tio.http.server.mvc.Routes;
import org.tio.utils.hutool.StrUtil;

import java.lang.reflect.Method;

/**
 * Created by litonglinux@qq.com on 11/9/2023_2:22 AM
 */
public class TioHandlerUtil {
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
}
