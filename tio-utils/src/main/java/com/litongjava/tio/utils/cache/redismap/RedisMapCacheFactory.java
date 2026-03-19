package com.litongjava.tio.utils.cache.redismap;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.CacheRemovalListener;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;

/**
 * Factory for creating and managing Redis-based caches.
 */
public enum RedisMapCacheFactory implements CacheFactory {
  INSTANCE;

  private final Map<String, RedisMapCache> map = new ConcurrentHashMap<>();

  private RedisMapCacheFactory() {
  }

  @Override
  public RedisMapCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    return register(cacheName, timeToLiveSeconds, timeToIdleSeconds, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> RedisMapCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds, RemovalListenerWrapper<T> removalListenerWrapper) {
    CacheRemovalListener<String, Serializable> listener = null;
    if (removalListenerWrapper != null && removalListenerWrapper.getListener() instanceof CacheRemovalListener) {
      listener = (CacheRemovalListener<String, Serializable>) removalListenerWrapper.getListener();
    }
    CacheRemovalListener<String, Serializable> thatListener = listener;
    Function<? super String, ? extends RedisMapCache> mappingFunction = k -> {
      RedisMapCache redisMapCache = new RedisMapCache(k, timeToLiveSeconds, timeToIdleSeconds, thatListener);
      return redisMapCache;
    };

    return map.computeIfAbsent(cacheName, mappingFunction);
  }

  @Override
  public RedisMapCache getCache(String cacheName, boolean skipNull) {
    RedisMapCache cache = map.get(cacheName);
    if (cache == null && !skipNull) {
      throw new IllegalArgumentException("Cache with name " + cacheName + " does not exist.");
    }
    return cache;
  }

  @Override
  public RedisMapCache getCache(String cacheName) {
    return getCache(cacheName, true);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public RedisMapCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds(), null);
  }
}
