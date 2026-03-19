package com.litongjava.tio.utils.cache.caffeine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;
import com.litongjava.tio.utils.caffeine.CaffeineUtils;

public enum CaffeineCacheFactory implements CacheFactory {
  INSTANCE;

  private Logger log = LoggerFactory.getLogger(CaffeineCacheFactory.class);
  private Map<String, CaffeineCache> map = new HashMap<>();
  private Object lock = new Object();

  /**
   * timeToLiveSeconds和timeToIdleSeconds不允许同时为null
   * @param cacheName
   * @param timeToLiveSeconds
   * @param timeToIdleSeconds
   * @return
   * @author tanyaowu
   */
  public CaffeineCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    return register(cacheName, timeToLiveSeconds, timeToIdleSeconds, null);
  }

  @SuppressWarnings("unchecked")
  public <T> CaffeineCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper) {

    RemovalListener<String, Serializable> removalListener = null;
    if (removalListenerWrapper != null) {
      // 检查 removalListenerWrapper 是否持有 RemovalListener 类型的监听器
      if (removalListenerWrapper.getListener() instanceof RemovalListener) {
        removalListener = (RemovalListener<String, Serializable>) removalListenerWrapper.getListener();
      }
    }

    CaffeineCache caffeineCache = map.get(cacheName);
    if (caffeineCache == null) {
      synchronized (lock) {
        caffeineCache = map.get(cacheName);
        if (caffeineCache == null) {
          Integer initialCapacity = 10;
          Integer maximumSize = 5000000;
          boolean recordStats = false;
          LoadingCache<String, Serializable> loadingCache = CaffeineUtils.createLoadingCache(cacheName,
              timeToLiveSeconds, timeToIdleSeconds, initialCapacity, maximumSize, recordStats, removalListener);

          Integer temporaryMaximumSize = 500000;
          LoadingCache<String, Serializable> temporaryLoadingCache = CaffeineUtils.createLoadingCache(cacheName, 10L,
              (Long) null, initialCapacity, temporaryMaximumSize, recordStats, removalListener);
          caffeineCache = new CaffeineCache(cacheName, loadingCache, temporaryLoadingCache);

          caffeineCache.setTimeToIdleSeconds(timeToIdleSeconds);
          caffeineCache.setTimeToLiveSeconds(timeToLiveSeconds);

          map.put(cacheName, caffeineCache);
        }
      }
    }
    return caffeineCache;
  }

  public CaffeineCache getCache(String cacheName, boolean skipNull) {
    CaffeineCache caffeineCache = map.get(cacheName);
    if (caffeineCache == null && !skipNull) {
      log.error("cacheName[{}] is not yet registered, please register first.", cacheName);
    }
    return caffeineCache;
  }

  public CaffeineCache getCache(String cacheName) {
    return getCache(cacheName, false);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public CaffeineCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds(), null);
  }

}
