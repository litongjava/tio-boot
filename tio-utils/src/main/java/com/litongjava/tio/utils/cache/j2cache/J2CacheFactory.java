package com.litongjava.tio.utils.cache.j2cache;

import java.util.Map;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.CacheName;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;

public class J2CacheFactory implements CacheFactory{

  @Override
  public AbsCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> AbsCache register(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      RemovalListenerWrapper<T> removalListenerWrapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AbsCache getCache(String cacheName, boolean skipNull) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public J2Cache getCache(String cacheName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, ? extends AbsCache> getMap() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AbsCache register(CacheName cacheName) {
    // TODO Auto-generated method stub
    return null;
  }

}
