package com.litongjava.tio.utils.cache.redis;

import java.util.HashMap;
import java.util.Map;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;

public enum RedisCacheFactory implements CacheFactory {
  INSTANCE;

  private Logger log = LoggerFactory.getLogger(RedisCacheFactory.class);
  private Map<String, TioRedisCache> map = new HashMap<>();
  private RedissonClient redisson;
  private Object lock = new Object();

  public RedissonClient getRedisson() {
    return redisson;
  }

  public void setRedisson(RedissonClient redisson) {
    this.redisson = redisson;
  }

  /**
   * timeToLiveSeconds和timeToIdleSeconds不允许同时为null
   * @param cacheName
   * @param timeToLiveSeconds
   * @param timeToIdleSeconds
   * @return
   * @author tanyaowu
   */
  @Override
  public TioRedisCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    RedisExpireUpdateTask.start(this);

    TioRedisCache redisCache = map.get(cacheName);
    if (redisCache == null) {
      synchronized (lock) {
        redisCache = map.get(cacheName);
        if (redisCache == null) {
          redisCache = new TioRedisCache(redisson, cacheName, timeToLiveSeconds, timeToIdleSeconds);

          redisCache.setTimeToIdleSeconds(timeToIdleSeconds);
          redisCache.setTimeToLiveSeconds(timeToLiveSeconds);
          map.put(cacheName, redisCache);
        }
      }
    }
    return redisCache;
  }

  @Override
  public <T> TioRedisCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds, RemovalListenerWrapper<T> removalListenerWrapper) {
    return null;
  }

  @Override
  public TioRedisCache getCache(String cacheName, boolean skipNull) {
    TioRedisCache redisCache = map.get(cacheName);
    if (redisCache == null) {
      log.error("cacheName[{}] is not yet registered, please register first.", cacheName);
    }
    return redisCache;
  }

  @Override
  public TioRedisCache getCache(String cacheName) {
    return map.get(cacheName);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public TioRedisCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds());
  }
}
