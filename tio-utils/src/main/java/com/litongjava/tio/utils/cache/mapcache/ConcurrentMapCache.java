package com.litongjava.tio.utils.cache.mapcache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheRemovalListener;
import com.litongjava.tio.utils.cache.RemovalCause;

public class ConcurrentMapCache extends AbsCache {
  private CacheRemovalListener<String, Serializable> removalListener;
  private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> expirationTimes = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public ConcurrentMapCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      CacheRemovalListener<String, Serializable> removalListener) {
    super(cacheName, timeToLiveSeconds, timeToIdleSeconds);
    this.removalListener = removalListener;
  }

  @Override
  public void clear() {
    map.clear();
    expirationTimes.clear();
  }

  @Override
  public Serializable _get(String key) {
    Serializable value = map.get(key);
    if (value != null && getTimeToIdleSeconds() != null) {
      // 更新 TTI 过期时间
      long newExpirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(getTimeToIdleSeconds());
      expirationTimes.put(key, newExpirationTime);
    }
    return value;
  }

  @Override
  public Iterable<String> keys() {
    KeySetView<String, Serializable> keySet = map.keySet();
    return keySet;
  }

  @Override
  public Collection<String> keysCollection() {
    return map.keySet();
  }

  @Override
  public void put(String key, Serializable value) {
    map.put(key, value);
    scheduleExpiration(key, getTimeToLiveSeconds());
  }

  @Override
  public void remove(String key) {
    Serializable value = map.remove(key);
    expirationTimes.remove(key);
    if (removalListener != null && value != null) {
      removalListener.onCacheRemoval(key, value, RemovalCause.EXPLICIT);
    }
  }

  @Override
  public void putTemporary(String key, Serializable value) {
    map.put(key, value);
    scheduleExpiration(key, (long) MAX_EXPIRE_IN_LOCAL); // 临时条目的过期时间
  }

  @Override
  public long ttl(String key) {
    Long expirationTime = expirationTimes.get(key);
    return expirationTime != null ? expirationTime - System.currentTimeMillis() : -1;
  }

  private void scheduleExpiration(final String key, Long ttl) {
    long ttlExpirationTime = ttl != null ? System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ttl) : Long.MAX_VALUE;
    long ttiExpirationTime = getTimeToIdleSeconds() != null
        ? System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(getTimeToIdleSeconds())
        : Long.MAX_VALUE;
    long expirationTime = Math.min(ttlExpirationTime, ttiExpirationTime);
    expirationTimes.put(key, expirationTime);

    scheduler.schedule(() -> {
      Long storedExpirationTime = expirationTimes.get(key);
      if (storedExpirationTime != null && System.currentTimeMillis() >= storedExpirationTime) {
        Serializable value = map.remove(key);
        expirationTimes.remove(key);
        if (removalListener != null && value != null) {
          RemovalCause cause = System.currentTimeMillis() >= ttlExpirationTime ? RemovalCause.EXPIRED
              : RemovalCause.EVICTED;
          removalListener.onCacheRemoval(key, value, cause);
        }
      }
    }, expirationTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public Map<String, Serializable> asMap() {
    return map;
  }

  @Override
  public long size() {
    return map.size();
  }

}
