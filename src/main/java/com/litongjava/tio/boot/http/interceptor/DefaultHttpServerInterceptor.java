package com.litongjava.tio.boot.http.interceptor;

import java.util.Map;
import java.util.Map.Entry;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpServerInterceptor;

/**
 * dispather
 * @author Tong Li
 *
 */
public class DefaultHttpServerInterceptor implements HttpServerInterceptor {

  /**
   * /* 表示匹配任何以特定路径开始的路径，/** 表示匹配该路径及其下的任何子路径
   * @param path
   * @return
   */
  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache)
      throws Exception {
    String path = requestLine.getPath();
    ServerInteceptorConfigure config = Aop.get(ServerInteceptorConfigure.class);
    Map<String, Class<? extends HttpServerInterceptor>> inteceptors = config.getInteceptors();

    // Check for wildcard matches
    for (Entry<String, Class<? extends HttpServerInterceptor>> entry : inteceptors.entrySet()) {
      String key = entry.getKey();

      Class<? extends HttpServerInterceptor> inteceptorClaszz = null;
      if (key.endsWith("/**")) {
        String baseRoute = key.substring(0, key.length() - 2);
        if (path.startsWith(baseRoute)) {
          inteceptorClaszz = entry.getValue();

        }
      } else if (key.endsWith("/*")) {
        String baseRoute = key.substring(0, key.length() - 1);
        if (path.startsWith(baseRoute)) {
          inteceptorClaszz = entry.getValue();
        }
      } else if (path.equals(key)) {
        inteceptorClaszz = entry.getValue();
      }

      if (inteceptorClaszz != null) {
        HttpServerInterceptor handler = Aop.get(inteceptorClaszz);
        if (handler != null) {
          HttpResponse response = handler.doBeforeHandler(request, requestLine, responseFromCache);
          if (response != null) {
            return response;
          }

        }
      }
    }
    return null;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception {
    String path = requestLine.getPath();
    ServerInteceptorConfigure config = Aop.get(ServerInteceptorConfigure.class);
    Map<String, Class<? extends HttpServerInterceptor>> inteceptors = config.getInteceptors();

    // Check for wildcard matches
    for (Entry<String, Class<? extends HttpServerInterceptor>> entry : inteceptors.entrySet()) {
      String key = entry.getKey();

      Class<? extends HttpServerInterceptor> inteceptorClaszz = null;
      if (key.endsWith("/**")) {
        String baseRoute = key.substring(0, key.length() - 2);
        if (path.startsWith(baseRoute)) {
          inteceptorClaszz = entry.getValue();

        }
      } else if (key.endsWith("/*")) {
        String baseRoute = key.substring(0, key.length() - 1);
        if (path.startsWith(baseRoute)) {
          inteceptorClaszz = entry.getValue();
        }
      } else if (path.equals(key)) {
        inteceptorClaszz = entry.getValue();
      }

      if (inteceptorClaszz != null) {
        HttpServerInterceptor handler = Aop.get(inteceptorClaszz);
        if (handler != null) {
          handler.doAfterHandler(request, requestLine, response, cost);
        }
      }
    }
  }
}