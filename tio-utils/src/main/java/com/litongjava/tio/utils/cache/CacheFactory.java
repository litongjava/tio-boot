package com.litongjava.tio.utils.cache;

import java.util.Map;

public interface CacheFactory {

  /**
   * 注册并获取一个 Cache 实例。
   * 如果实例已存在，则返回现有实例；如果不存在，则创建新实例。
   *
   * @param cacheName 缓存名称
   * @param timeToLiveSeconds 存活时间（秒）
   * @param timeToIdleSeconds 空闲时间（秒）
   * @return ConcurrentMapCache 实例
   */
  public AbsCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds);

  public AbsCache register(CacheName cacheName);

  public <T> AbsCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper);

  public AbsCache getCache(String cacheName, boolean skipNull);

  public AbsCache getCache(String cacheName);

  public Map<String, ? extends AbsCache> getMap();
}
