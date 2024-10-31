package com.litongjava.tio.boot.http.interceptor;

import java.util.List;
import java.util.Map;

import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

/**
 * DefaultHttpServerInterceptor
 * @author Tong Li
 *
 */
public class DefaultHttpRequestInterceptorDispatcher implements HttpRequestInterceptor {
  private HttpInteceptorConfigure configure = null;
  public static final String static_file_reges = ".*\\.[a-zA-Z0-9]+$";

  /**
   * /* 表示匹配任何以特定路径开始的路径，/** 表示匹配该路径及其下的任何子路径
   * @param path
   * @return
   */
  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) throws Exception {
    if (configure == null) {
      configure = TioBootServer.me().getHttpInteceptorConfigure();
      if (configure == null) {
        return null;
      }
    }

    Map<String, HttpInterceptorModel> inteceptors = configure.getInteceptors();
    String path = requestLine.getPath();
    for (HttpInterceptorModel model : inteceptors.values()) {
      boolean isBlock = isMatched(path, model);
      if (isBlock) {
        HttpRequestInterceptor interceptor = model.getInterceptor();
        if (interceptor != null) {
          HttpResponse response = interceptor.doBeforeHandler(request, requestLine, responseFromCache);
          if (response != null) {
            return response; // 如果拦截器返回响应，直接返回
          }
        }
      }
    }
    return null; // 没有拦截器处理，继续后续流程
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost) throws Exception {
    if (configure == null) {
      configure = TioBootServer.me().getHttpInteceptorConfigure();
      if (configure == null) {
        return;
      }
    }

    Map<String, HttpInterceptorModel> inteceptors = configure.getInteceptors();
    String path = requestLine.getPath();
    for (HttpInterceptorModel model : inteceptors.values()) {
      boolean isBlock = isMatched(path, model);
      if (isBlock) {
        HttpRequestInterceptor interceptor = model.getInterceptor();
        if (interceptor != null) {
          interceptor.doAfterHandler(request, requestLine, response, cost);
        }
      }
    }

  }

  /**
   * 判断是否Mathch
   * @param path
   * @param model
   * @return
   */
  private boolean isMatched(String path, HttpInterceptorModel model) {
    List<String> blockedUrls = model.getBlockedUrls();
    List<String> allowedUrls = model.getAllowedUrls();

    // 优先检查 allowedUrls
    boolean alloweStaticFile = model.isAlloweStaticFile();
    boolean isAllowed = (allowedUrls != null && !allowedUrls.isEmpty()) && isUrlAllowed(path, allowedUrls, alloweStaticFile);
    if (isAllowed) {
      return false; // 被允许的路径不再检查 blockedUrls
    }

    boolean isBlocked = (blockedUrls != null && !blockedUrls.isEmpty()) && isUrlBlocked(path, blockedUrls, alloweStaticFile);
    return isBlocked;
  }

  private boolean isUrlBlocked(String path, List<String> blockedUrls, boolean isAlloweStaticFile) {
    return blockedUrls.stream().anyMatch(urlPattern -> pathMatchesPattern(path, urlPattern, isAlloweStaticFile));
  }

  private boolean isUrlAllowed(String path, List<String> allowedUrls, boolean isAlloweStaticFile) {
    return allowedUrls.stream().anyMatch(urlPattern -> pathMatchesPattern(path, urlPattern, isAlloweStaticFile));
  }

  private boolean pathMatchesPattern(String path, String pattern, boolean isAlloweStaticFile) {
    if (isAlloweStaticFile && path.matches(static_file_reges)) {
      return true; // 如果静态文件允许，直接放行带扩展名的路径
    }

    // 通配符匹配
    if (pattern.endsWith("/**")) {
      return path.startsWith(pattern.substring(0, pattern.length() - 3));
    } else if (pattern.endsWith("/*")) {
      return path.startsWith(pattern.substring(0, pattern.length() - 2));
    }
    return path.equals(pattern);
  }

}