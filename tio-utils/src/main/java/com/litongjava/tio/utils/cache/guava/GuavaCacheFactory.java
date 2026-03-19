package com.litongjava.tio.utils.cache.guava;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;
import com.litongjava.tio.utils.guava.GuavaUtils;

public enum GuavaCacheFactory implements CacheFactory {
  INSTANCE;

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private Map<String, GuavaCache> map = new HashMap<>();
  private Object lock = new Object();

  /**
   * timeToLiveSeconds和timeToIdleSeconds不允许同时为null
   * @param cacheName
   * @param timeToLiveSeconds
   * @param timeToIdleSeconds
   * @return
   * @author tanyaowu
   */
  @Override
  public GuavaCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    return register(cacheName, timeToLiveSeconds, timeToIdleSeconds, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> GuavaCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper) {

    RemovalListener<String, Serializable> removalListener = null;
    if (removalListenerWrapper.getListener() instanceof RemovalListener) {
      removalListener = (RemovalListener<String, Serializable>) removalListenerWrapper.getListener();
    }

    GuavaCache guavaCache = map.get(cacheName);
    if (guavaCache == null) {
      synchronized (lock) {
        guavaCache = map.get(cacheName);
        if (guavaCache == null) {
          Integer concurrencyLevel = 8;
          Integer initialCapacity = 10;
          Integer maximumSize = 5000000;
          boolean recordStats = false;
          Integer temporaryMaximumSize = 500000;

          LoadingCache<String, Serializable> loadingCache = GuavaUtils.createLoadingCache(concurrencyLevel,
              timeToLiveSeconds, timeToIdleSeconds, initialCapacity, maximumSize, recordStats, removalListener);

          LoadingCache<String, Serializable> temporaryLoadingCache = GuavaUtils.createLoadingCache(concurrencyLevel,
              10L, (Long) null, initialCapacity, temporaryMaximumSize, recordStats, removalListener);
          guavaCache = new GuavaCache(cacheName, loadingCache, temporaryLoadingCache);

          guavaCache.setTimeToIdleSeconds(timeToIdleSeconds);
          guavaCache.setTimeToLiveSeconds(timeToLiveSeconds);

          map.put(cacheName, guavaCache);
        }
      }
    }
    return guavaCache;
  }

  @Override
  public GuavaCache getCache(String cacheName, boolean skipNull) {
    GuavaCache guavaCache = map.get(cacheName);
    if (guavaCache == null) {
      log.error("cacheName[{}] is not yet registered, please register first.", cacheName);
    }
    return guavaCache;
  }

  @Override
  public GuavaCache getCache(String cacheName) {
    return map.get(cacheName);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public GuavaCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds(), null);
  }

}
