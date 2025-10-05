package com.litongjava.tio.boot.http.interceptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

/**
 * DefaultHttpServerInterceptor
 * 
 * @author Tong Li
 *
 */
public class DefaultHttpRequestInterceptorDispatcher implements HttpRequestInterceptor {
  private HttpInteceptorConfigure configure = null;
  public static final String static_file_reges = ".*\\.[a-zA-Z0-9]+$";
  private final Map<String, PathPattern> cache = new ConcurrentHashMap<>();

  /**
   * /* 表示匹配任何以特定路径开始的路径，/** 表示匹配该路径及其下的任何子路径
   * 
   * @param path
   * @return
   */
  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache)
      throws Exception {
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
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception {
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

  private boolean isMatched(String path, HttpInterceptorModel model) {

    // 先检查允许名单
    List<String> allowedUrls = model.getAllowedUrls();
    if (isUrlAllowed(path, allowedUrls)) {
      return false; // 白名单优先，直接放行
    }

    // 静态文件放行
    boolean alloweStaticFile = model.isAlloweStaticFile();
    // 1) 静态文件（如 .js/.  css/.png 等）直接放行
    if (alloweStaticFile && path.matches(static_file_reges)) {
      return false;
    }

    // 再检查拦截名单
    List<String> blockedUrls = model.getBlockedUrls();
    if (isUrlBlocked(path, blockedUrls)) {
      return true; // 命中黑名单
    }

    return false;
  }

  private boolean isUrlBlocked(String path, List<String> blockedUrls) {
    if (blockedUrls == null || blockedUrls.isEmpty()) {
      return false;
    }
    for (String urlPattern : blockedUrls) {
      if (pathMatchesPattern(path, urlPattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean isUrlAllowed(String path, List<String> allowedUrls) {
    if (allowedUrls == null || allowedUrls.isEmpty()) {
      return false;
    }
    for (String urlPattern : allowedUrls) {
      if (pathMatchesPattern(path, urlPattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean pathMatchesPattern(String path, String pattern) {
    // 2) 使用 PathPattern 支持 /**、/*、精确匹配、{var}、{var:regex}、段级可选 ?
    if (pattern == null || pattern.isEmpty()) {
      return false;
    }
    // 判断是否匹配
    PathPattern compiled = cache.computeIfAbsent(pattern, PathPattern::compile);
    return compiled.matches(path);
  }

}