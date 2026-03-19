package com.litongjava.tio.boot.http.handler.internal;

public class DefaultHttpRequestConstants {
  public static final String SESSION_RATE_LIMITER_KEY_SPLIT = "?";

  /**
   * 静态资源的CacheName
   * key:   path 譬如"/index.html"
   * value: FileCache
   */
  public static final String STATIC_RES_CONTENT_CACHE_NAME = "TIO_HTTP_STATIC_RES_CONTENT";
  public static final String SESSION_RATE_LIMITER_CACHE_NAME = "TIO_HTTP_SESSION_RATE_LIMITER";
  /**
   * 把cookie对象存到ChannelContext对象中
   * request.channelContext.setAttribute(SESSION_COOKIE_KEY, sessionCookie);
   */
  public static final String SESSION_COOKIE_KEY = "TIO_HTTP_SESSION_COOKIE";

}
