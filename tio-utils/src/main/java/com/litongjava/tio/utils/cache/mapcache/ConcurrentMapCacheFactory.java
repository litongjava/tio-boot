package com.litongjava.tio.utils.cache.mapcache;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.CacheRemovalListener;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;

/**
 * 使用Map保存数据
 * @author Tong Li
 *
 */
public enum ConcurrentMapCacheFactory implements CacheFactory {
  INSTANCE;

  private Map<String, ConcurrentMapCache> map = new ConcurrentHashMap<>();

  @Override
  public ConcurrentMapCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    return map.computeIfAbsent(cacheName,
        k -> new ConcurrentMapCache(cacheName, timeToLiveSeconds, timeToIdleSeconds, null));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ConcurrentMapCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper) {

    if (removalListenerWrapper != null) {
      if (removalListenerWrapper.getListener() instanceof CacheRemovalListener) {
        return map.computeIfAbsent(cacheName, k -> new ConcurrentMapCache(cacheName, timeToLiveSeconds,
            timeToIdleSeconds, (CacheRemovalListener<String, Serializable>) removalListenerWrapper.getListener()));
      } else {
        return map.computeIfAbsent(cacheName,
            k -> new ConcurrentMapCache(cacheName, timeToLiveSeconds, timeToIdleSeconds, null));
      }
    } else {
      return map.computeIfAbsent(cacheName,
          k -> new ConcurrentMapCache(cacheName, timeToLiveSeconds, timeToIdleSeconds, null));
    }

  }

  @Override
  public ConcurrentMapCache getCache(String cacheName, boolean skipNull) {
    return map.get(cacheName);
  }

  @Override
  public ConcurrentMapCache getCache(String cacheName) {
    return map.get(cacheName);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public ConcurrentMapCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds(), null);
  }
}
