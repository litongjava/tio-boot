package com.litongjava.tio.utils.caffeine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.litongjava.tio.utils.cache.caffeine.DefaultRemovalListener;

/**
 * @author tanyaowu
 *
 */
public class CaffeineUtils {

  private static final ConcurrentHashMap<String, LoadingCache<?, ?>> cacheMap = new ConcurrentHashMap<>();

  /**
   * 
   */
  public CaffeineUtils() {
  }

  /**
   * @param cacheName
   * @param timeToLiveSeconds 设置写缓存后过期时间（单位：秒）
   * @param timeToIdleSeconds 设置读缓存后过期时间（单位：秒）
   * @param initialCapacity
   * @param maximumSize
   * @param recordStats
   * @return
   */
  public static <K, V> LoadingCache<K, V> createLoadingCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds, Integer initialCapacity, Integer maximumSize, boolean recordStats) {
    return createLoadingCache(cacheName, timeToLiveSeconds, timeToIdleSeconds, initialCapacity, maximumSize, recordStats, null);
  }

  /**
   * @param cacheName
   * @param timeToLiveSeconds 设置写缓存后过期时间（单位：秒）
   * @param timeToIdleSeconds 设置读缓存后过期时间（单位：秒）
   * @param initialCapacity
   * @param maximumSize
   * @param recordStats
   * @param removalListener
   * @return
   */
  public static <K, V> LoadingCache<K, V> createLoadingCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds, Integer initialCapacity, Integer maximumSize, boolean recordStats,
      RemovalListener<K, V> removalListener) {

    if (removalListener == null) {
      removalListener = new DefaultRemovalListener<K, V>(cacheName);
    }

    Caffeine<K, V> cacheBuilder = Caffeine.newBuilder().removalListener(removalListener);

    // 设置并发级别为8，并发级别是指可以同时写缓存的线程数
    // cacheBuilder.concurrencyLevel(concurrencyLevel);
    if (timeToLiveSeconds != null && timeToLiveSeconds > 0) {
      // 设置写缓存后8秒钟过期
      cacheBuilder.expireAfterWrite(timeToLiveSeconds, TimeUnit.SECONDS);
    }
    if (timeToIdleSeconds != null && timeToIdleSeconds > 0) {
      // 设置访问缓存后8秒钟过期
      cacheBuilder.expireAfterAccess(timeToIdleSeconds, TimeUnit.SECONDS);
    }

    // 设置缓存容器的初始容量为10
    cacheBuilder.initialCapacity(initialCapacity);
    // 设置缓存最大容量为100，超过100之后就会按照LRU最近最少使用算法来移除缓存项
    cacheBuilder.maximumSize(maximumSize);

    if (recordStats) {
      // 设置要统计缓存的命中率
      cacheBuilder.recordStats();
    }
    // build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
    LoadingCache<K, V> loadingCache = cacheBuilder.build(new CacheLoader<K, V>() {
      @Override
      public V load(K key) throws Exception {
        return null;
      }
    });

    return loadingCache;
  }

  /**
   * 获取缓存实例，如果不存在则自动创建
   *
   * @param name 缓存名称
   * @param timeToLiveSeconds 写缓存后过期时间
   * @param timeToIdleSeconds 读缓存后过期时间
   * @param initialCapacity 缓存初始容量
   * @param maximumSize 缓存最大容量
   * @param recordStats 是否记录缓存命中率
   * @return 缓存实例
   */
  @SuppressWarnings("unchecked")
  public static <K, V> LoadingCache<K, V> getCache(String name, Long timeToLiveSeconds, Long timeToIdleSeconds, Integer initialCapacity, Integer maximumSize, boolean recordStats) {
    return (LoadingCache<K, V>) cacheMap.computeIfAbsent(name, key -> createLoadingCache(name, timeToLiveSeconds, timeToIdleSeconds, initialCapacity, maximumSize, recordStats));
  }

  @SuppressWarnings("unchecked")
  public static <K, V> LoadingCache<K, V> getCache(String name) {
    Long timeToLiveSeconds = 5 * 60L; // 写缓存后N秒过期
    Long timeToIdleSeconds = 5 * 60L; // 读缓存后N秒过期
    Integer initialCapacity = 500; // 初始容量
    Integer maximumSize = Integer.MAX_VALUE; // 最大容量
    boolean recordStats = true; // 记录缓存命中率

    return (LoadingCache<K, V>) cacheMap.computeIfAbsent(name, key -> createLoadingCache(name, timeToLiveSeconds, timeToIdleSeconds, initialCapacity, maximumSize, recordStats));
  }
}
