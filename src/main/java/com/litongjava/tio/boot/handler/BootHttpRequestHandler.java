package com.litongjava.tio.boot.handler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.RequestLine;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.server.intf.HttpServerInterceptor;
import org.tio.http.server.intf.HttpSessionListener;
import org.tio.http.server.intf.ThrowableHandler;
import org.tio.http.server.mvc.Routes;
import org.tio.http.server.mvc.intf.ControllerFactory;
import org.tio.http.server.session.SessionCookieDecorator;
import org.tio.http.server.stat.ip.path.IpPathAccessStats;
import org.tio.http.server.stat.token.TokenPathAccessStats;
import org.tio.http.server.util.Resps;
import org.tio.utils.cache.caffeine.CaffeineCache;
import org.tio.utils.hutool.StrUtil;

import com.esotericsoftware.reflectasm.MethodAccess;

/**
 *
 * @author litongjava
 *
 */
public class BootHttpRequestHandler implements HttpRequestHandler {
  private Logger log = LoggerFactory.getLogger(BootHttpRequestHandler.class);
  /**
   * 静态资源的CacheName
   * key:   path 譬如"/index.html"
   * value: FileCache
   */
  private static final String STATIC_RES_CONTENT_CACHENAME = "TIO_HTTP_STATIC_RES_CONTENT";
  private static final String SESSIONRATELIMITER_CACHENAME = "TIO_HTTP_SESSIONRATELIMITER_CACHENAME";
  /**
   * 把cookie对象存到ChannelContext对象中
   * request.channelContext.setAttribute(SESSION_COOKIE_KEY, sessionCookie);
   */
  private static final String SESSION_COOKIE_KEY = "TIO_HTTP_SESSION_COOKIE";
  private static final Map<Class<?>, MethodAccess> CLASS_METHODACCESS_MAP = new HashMap<>();
  protected HttpConfig httpConfig;
  protected Routes routes = null;
  private HttpServerInterceptor httpServerInterceptor;
  private HttpSessionListener httpSessionListener;
  private ThrowableHandler throwableHandler;
  private SessionCookieDecorator sessionCookieDecorator;
  private IpPathAccessStats ipPathAccessStats;
  private TokenPathAccessStats tokenPathAccessStats;
  /**
   * 静态资源缓存
   */
  CaffeineCache staticResCache;
  /**
   * 限流缓存
   */
  private CaffeineCache sessionRateLimiterCache;
  private static final String SESSIONRATELIMITER_KEY_SPLIT = "?";
  private String contextPath;
  private int contextPathLength = 0;
  private String suffix;
  private int suffixLength = 0;
  /**
   * 赋值兼容处理
   */
  private boolean compatibilityAssignment = true;

  public BootHttpRequestHandler(HttpConfig httpConfig, Class<?>[] scanRootClasses) throws Exception {
    this(httpConfig, scanRootClasses, null);
  }

  public BootHttpRequestHandler(HttpConfig httpConfig, Class<?>[] scanRootClasses,
      ControllerFactory controllerFactory) {
    Routes routes = new Routes(scanRootClasses, controllerFactory);
    init(httpConfig, routes);
  }

  private void init(HttpConfig httpConfig, Routes routes) {
    if (httpConfig == null) {
      throw new RuntimeException("httpConfig can not be null");
    }
    this.contextPath = httpConfig.getContextPath();
    this.suffix = httpConfig.getSuffix();

    if (StrUtil.isNotBlank(contextPath)) {
      contextPathLength = contextPath.length();
    }
    if (StrUtil.isNotBlank(suffix)) {
      suffixLength = suffix.length();
    }

    this.httpConfig = httpConfig;

    if (httpConfig.getMaxLiveTimeOfStaticRes() > 0) {
      staticResCache = CaffeineCache.register(STATIC_RES_CONTENT_CACHENAME,
          (long) httpConfig.getMaxLiveTimeOfStaticRes(), null);
    }

    // sessionRateLimiterCache = CaffeineCache.register(SESSIONRATELIMITER_CACHENAME, 60 * 1L, null);

    this.routes = routes;

  }

  @Override
  public HttpResponse handler(HttpRequest packet) throws Exception {
    return null;
  }

  @Override
  public HttpResponse resp404(HttpRequest request, RequestLine requestLine) throws Exception {
    if (routes != null) {
      String page404 = httpConfig.getPage404();
      Method method = routes.PATH_METHOD_MAP.get(page404);
      if (method != null) {
        return Resps.forward(request, page404);
      }
    }

    return Resps.resp404(request, requestLine, httpConfig);
  }

  @Override
  public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable) throws Exception {
    if (throwableHandler != null) {
      return throwableHandler.handler(request, requestLine, throwable);
    }

    if (routes != null) {
      String page500 = httpConfig.getPage500();
      Method method = routes.PATH_METHOD_MAP.get(page500);
      if (method != null) {
        return Resps.forward(request, page500);
      }
    }

    return Resps.resp500(request, requestLine, httpConfig, throwable);
  }

  @Override
  public HttpConfig getHttpConfig(HttpRequest request) {
    return httpConfig;
  }

  @Override
  public void clearStaticResCache() {
    if (staticResCache != null) {
      staticResCache.clear();
    }
  }
}
