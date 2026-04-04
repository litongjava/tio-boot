package com.litongjava.tio.boot.cache;

import com.litongjava.tio.utils.cache.AbsCache;

import nexus.io.tio.http.common.HttpConfig;

public class StaticResourcesCache {

  /**
   * Static resource cache.
   */
  private static AbsCache staticResCache;
  private static HttpConfig httpConfig;

  public static AbsCache getStaticResCache() {
    return staticResCache;
  }

  public static void setStaticResCache(AbsCache staticResCache) {
    StaticResourcesCache.staticResCache = staticResCache;
  }

  public static HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public static void setHttpConfig(HttpConfig httpConfig) {
    StaticResourcesCache.httpConfig = httpConfig;
  }

}
