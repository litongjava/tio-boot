package com.litongjava.tio.boot.http.handler;

public class DefaultHttpRequestConstants {
  public static final String SESSION_RATE_LIMITER_KEY_SPLIT = "?";

  /**
   * 静态资源的CacheName
   * key:   path 譬如"/index.html"
   * value: FileCache
   */
  public static final String STATIC_RES_CONTENT_CACHENAME = "TIO_HTTP_STATIC_RES_CONTENT";
  public static final String SESSION_RATE_LIMITER_CACHENAME = "TIO_HTTP_SESSIONRATELIMITER_CACHENAME";
  /**
   * 把cookie对象存到ChannelContext对象中
   * request.channelContext.setAttribute(SESSION_COOKIE_KEY, sessionCookie);
   */
  public static final String SESSION_COOKIE_KEY = "TIO_HTTP_SESSION_COOKIE";

}
