package com.litongjava.tio.utils.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheNameService {
  private Map<String, CacheName> map = new ConcurrentHashMap<>();

  public Collection<CacheName> cacheNames() {
    return map.values();
  }

  public void add(CacheName cache) {
    map.put(cache.getName(), cache);
  }

  public CacheName get(String name) {
    return map.get(name);
  }

  public CacheName remove(String name) {
    return map.remove(name);
  }
}
