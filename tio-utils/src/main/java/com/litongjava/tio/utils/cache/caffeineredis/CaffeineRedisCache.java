package com.litongjava.tio.utils.cache.caffeineredis;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.redisson.api.RTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheChangeType;
import com.litongjava.tio.utils.cache.CacheChangedVo;
import com.litongjava.tio.utils.cache.caffeine.CaffeineCache;
import com.litongjava.tio.utils.cache.redis.TioRedisCache;
import com.litongjava.tio.utils.cache.redis.RedisExpireUpdateTask;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lock.LockUtils;

/**
 * @author tanyaowu
 * 2017年8月12日 下午9:13:54
 */
public class CaffeineRedisCache extends AbsCache {
  public static final String CACHE_CHANGE_TOPIC = "TIO_CACHE_CHANGE_TOPIC_CAFFEINE";

  private Logger log = LoggerFactory.getLogger(CaffeineRedisCache.class);
  CaffeineCache localCache = null;
  TioRedisCache distCache = null;
  private RTopic topic;

  /**
   * @param localCache
   * @param distCache
   * @author tanyaowu
   */
  public CaffeineRedisCache(String cacheName, CaffeineCache caffeineCache, TioRedisCache redisCache) {
    super(cacheName);
    this.localCache = caffeineCache;
    this.distCache = redisCache;
  }

  /**
   *
   * @author tanyaowu
   */
  @Override
  public void clear() {
    localCache.clear();
    distCache.clear();

    CacheChangedVo cacheChangedVo = new CacheChangedVo(cacheName, CacheChangeType.CLEAR);
    topic.publish(cacheChangedVo);
  }

  /**
   * @param key
   * @return
   * @author tanyaowu
   */
  @Override
  public Serializable _get(String key) {
    if (StrUtil.isBlank(key)) {
      return null;
    }

    Serializable ret = localCache.get(key);
    if (ret == null) {
      try {
        LockUtils.runWriteOrWaitRead("_tio_cr_" + key, this, () -> {
//					@Override
//					public void read() {
//					}

//					@Override
//					public void write() {
//						Serializable ret = localCache.get(key);
          if (localCache.get(key) == null) {
            Serializable ret1 = distCache.get(key);
            if (ret1 != null) {
              localCache.put(key, ret1);
            }
          }
//					}
        });
      } catch (Exception e) {
        log.error(e.toString(), e);
      }
      ret = localCache.get(key);// (Serializable) readWriteRet.writeRet;
    } else {// 在本地就取到数据了，那么需要在redis那定时更新一下过期时间
      Long timeToIdleSeconds = distCache.getTimeToIdleSeconds();
      if (timeToIdleSeconds != null) {
        RedisExpireUpdateTask.add(cacheName, key, timeToIdleSeconds);
      }
    }
    return ret;
  }

  /**
   * @return
   * @author tanyaowu
   */
  @Override
  public Iterable<String> keys() {
    return distCache.keys();
  }
  
  @Override
  public Collection<String> keysCollection() {
    return distCache.keysCollection();
  }

  /**
   * @param key
   * @param value
   * @author tanyaowu
   */
  @Override
  public void put(String key, Serializable value) {
    localCache.put(key, value);
    distCache.put(key, value);

    CacheChangedVo cacheChangedVo = new CacheChangedVo(cacheName, key, CacheChangeType.PUT);
    topic.publish(cacheChangedVo);
  }

  @Override
  public void putTemporary(String key, Serializable value) {
    localCache.putTemporary(key, value);
    distCache.putTemporary(key, value);

    //
    // CacheChangedVo cacheChangedVo = new CacheChangedVo(cacheName, key, CacheChangeType.PUT);
    // topic.publish(cacheChangedVo);
  }

  /**
   * @param key
   * @author tanyaowu
   */
  @Override
  public void remove(String key) {
    if (StrUtil.isBlank(key)) {
      return;
    }

    localCache.remove(key);
    distCache.remove(key);

    CacheChangedVo cacheChangedVo = new CacheChangedVo(cacheName, key, CacheChangeType.REMOVE);
    topic.publish(cacheChangedVo);
  }

  @Override
  public long ttl(String key) {
    return distCache.ttl(key);
  }

  public RTopic getTopic() {
    return topic;
  }

  public void setTopic(RTopic topic) {
    this.topic = topic;
  }

  @Override
  public Map<String, Serializable> asMap() {
    return distCache.asMap();
  }

  @Override
  public long size() {
    return distCache.size();
  }
 
}
