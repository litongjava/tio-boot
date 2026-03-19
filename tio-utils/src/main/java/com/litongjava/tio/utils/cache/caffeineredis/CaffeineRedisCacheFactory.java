package com.litongjava.tio.utils.cache.caffeineredis;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.model.cache.ICache;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheChangeType;
import com.litongjava.tio.utils.cache.CacheChangedVo;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCache;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCacheFactory;
import com.litongjava.tio.utils.cache.redis.TioRedisCache;
import com.litongjava.tio.utils.cache.redis.RedisCacheFactory;
import com.litongjava.tio.utils.hutool.StrUtil;

public enum CaffeineRedisCacheFactory implements CacheFactory {

  INSTANCE;

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private Object lock = new Object();
  private Map<String, CaffeineRedisCache> map = new HashMap<>();
  private boolean inited = false;
  private RedissonClient redisson;
  private RTopic topic;

  /**
   * 初始topic
   * @param redisson
   */
  public void init(RedissonClient redisson) {
    if (!inited) {
      synchronized (lock) {
        if (!inited) {
          this.redisson = redisson;
          topic = redisson.getTopic(CaffeineRedisCache.CACHE_CHANGE_TOPIC);
          addListener(topic);
          inited = true;
        }
      }
    }
  }

  private void addListener(RTopic topic) {
    topic.addListener(CacheChangedVo.class, new MessageListener<CacheChangedVo>() {
      @Override
      public void onMessage(CharSequence channel, CacheChangedVo cacheChangedVo) {
        String clientid = cacheChangedVo.getClientId();
        if (StrUtil.isBlank(clientid)) {
          log.error("clientid is null");
          return;
        }
        if (Objects.equals(CacheChangedVo.CLIENTID, clientid)) {
          log.debug("自己发布的消息,{}", clientid);
          return;
        }

        String cacheName = cacheChangedVo.getCacheName();
        CaffeineRedisCache caffeineRedisCache = getCache(cacheName);
        if (caffeineRedisCache == null) {
          log.info("不能根据cacheName[{}]找到CaffeineRedisCache对象", cacheName);
          return;
        }

        CacheChangeType type = cacheChangedVo.getType();
        if (type == CacheChangeType.PUT || type == CacheChangeType.UPDATE || type == CacheChangeType.REMOVE) {
          String key = cacheChangedVo.getKey();
          caffeineRedisCache.localCache.remove(key);
        } else if (type == CacheChangeType.CLEAR) {
          caffeineRedisCache.localCache.clear();
        }
      }
    });
  }

  @Override
  public CaffeineRedisCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    init(redisson);
    CaffeineRedisCache caffeineRedisCache = map.get(cacheName);
    if (caffeineRedisCache == null) {
      synchronized (CaffeineRedisCache.class) {
        caffeineRedisCache = map.get(cacheName);
        if (caffeineRedisCache == null) {
          RedisCacheFactory.INSTANCE.setRedisson(redisson);
          TioRedisCache redisCache = RedisCacheFactory.INSTANCE.register(cacheName, timeToLiveSeconds, timeToIdleSeconds);

          Long timeToLiveSecondsForCaffeine = timeToLiveSeconds;
          Long timeToIdleSecondsForCaffeine = timeToIdleSeconds;

          if (timeToLiveSecondsForCaffeine != null) {
            timeToLiveSecondsForCaffeine = Math.min(timeToLiveSecondsForCaffeine, ICache.MAX_EXPIRE_IN_LOCAL);
          }
          if (timeToIdleSecondsForCaffeine != null) {
            timeToIdleSecondsForCaffeine = Math.min(timeToIdleSecondsForCaffeine, ICache.MAX_EXPIRE_IN_LOCAL);
          }
          CaffeineCache caffeineCache = CaffeineCacheFactory.INSTANCE.register(cacheName, timeToLiveSecondsForCaffeine,
              timeToIdleSecondsForCaffeine);

          caffeineRedisCache = new CaffeineRedisCache(cacheName, caffeineCache, redisCache);
          caffeineRedisCache.setTopic(this.topic);

          caffeineRedisCache.setTimeToIdleSeconds(timeToIdleSeconds);
          caffeineRedisCache.setTimeToLiveSeconds(timeToLiveSeconds);

          map.put(cacheName, caffeineRedisCache);
        }
      }
    }
    return caffeineRedisCache;

  }

  @Override
  public <T> CaffeineRedisCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper) {
    return null;
  }

  @Override
  public CaffeineRedisCache getCache(String cacheName, boolean skipNull) {
    CaffeineRedisCache caffeineRedisCache = map.get(cacheName);
    if (caffeineRedisCache == null && !skipNull) {
      log.error("cacheName[{}] is not yet registered, please register first.", cacheName);
    }
    return caffeineRedisCache;
  }

  @Override
  public CaffeineRedisCache getCache(String cacheName) {
    return getCache(cacheName, false);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public CaffeineRedisCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds());
  }

}
