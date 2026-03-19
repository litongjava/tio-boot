package com.litongjava.tio.utils.cache.guavaredis;

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
import com.litongjava.tio.utils.cache.guava.GuavaCache;
import com.litongjava.tio.utils.cache.guava.GuavaCacheFactory;
import com.litongjava.tio.utils.cache.redis.TioRedisCache;
import com.litongjava.tio.utils.cache.redis.RedisCacheFactory;
import com.litongjava.tio.utils.hutool.StrUtil;

public enum GuavaRedisCacheFactory implements CacheFactory {

  INSTANCE;

  private Logger log = LoggerFactory.getLogger(this.getClass());
  //private Object lock = new Object();
  private Map<String, GuavaRedisCache> map = new HashMap<>();
  private boolean inited = false;
  private RedissonClient redisson;
  private RTopic topic;

  /**
   * 初始topic
   * @param redisson
   */
  public void init(RedissonClient redisson) {
    if (!inited) {
      synchronized (GuavaRedisCache.class) {
        if (!inited) {
          topic = redisson.getTopic(GuavaRedisCache.CACHE_CHANGE_TOPIC);
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
        GuavaRedisCache guavaRedisCache = getCache(cacheName);
        if (guavaRedisCache == null) {
          log.info("不能根据cacheName[{}]找到GuavaRedisCache对象", cacheName);
          return;
        }

        CacheChangeType type = cacheChangedVo.getType();
        if (type == CacheChangeType.PUT || type == CacheChangeType.UPDATE || type == CacheChangeType.REMOVE) {
          String key = cacheChangedVo.getKey();
          guavaRedisCache.guavaCache.remove(key);
        } else if (type == CacheChangeType.CLEAR) {
          guavaRedisCache.guavaCache.clear();
        }
      }
    });
  }

  @Override
  public GuavaRedisCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    init(redisson);
    GuavaRedisCache guavaRedisCache = map.get(cacheName);
    if (guavaRedisCache == null) {
      synchronized (GuavaRedisCache.class) {
        guavaRedisCache = map.get(cacheName);
        if (guavaRedisCache == null) {
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
          GuavaCache GuavaCache = GuavaCacheFactory.INSTANCE.register(cacheName, timeToLiveSecondsForCaffeine,
              timeToIdleSecondsForCaffeine);

          guavaRedisCache = new GuavaRedisCache(cacheName, GuavaCache, redisCache);
          guavaRedisCache.setTopic(this.topic);

          guavaRedisCache.setTimeToIdleSeconds(timeToIdleSeconds);
          guavaRedisCache.setTimeToLiveSeconds(timeToLiveSeconds);

          map.put(cacheName, guavaRedisCache);
        }
      }
    }
    return guavaRedisCache;

  }

  @Override
  public <T> GuavaRedisCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper) {
    return null;
  }

  @Override
  public GuavaRedisCache getCache(String cacheName, boolean skipNull) {
    GuavaRedisCache guavaRedisCache = map.get(cacheName);
    if (guavaRedisCache == null && !skipNull) {
      log.error("cacheName[{}] is not yet registered, please register first.", cacheName);
    }
    return guavaRedisCache;
  }

  @Override
  public GuavaRedisCache getCache(String cacheName) {
    return getCache(cacheName, false);
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    return map;
  }

  @Override
  public GuavaRedisCache register(CacheName cacheName) {
    return this.register(cacheName.getName(), cacheName.getTimeToLiveSeconds(), cacheName.getTimeToIdleSeconds());
  }

}
