package com.litongjava.tio.utils.cache.guava;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.LoadingCache;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 *
 * @author tanyaowu
 * 2017年8月5日 上午10:16:26
 */
public class GuavaCache extends AbsCache {

  private LoadingCache<String, Serializable> loadingCache = null;

  private LoadingCache<String, Serializable> temporaryLoadingCache = null;

  public GuavaCache(String cacheName, LoadingCache<String, Serializable> loadingCache,
      LoadingCache<String, Serializable> temporaryLoadingCache) {
    super(cacheName);
    this.loadingCache = loadingCache;
    this.temporaryLoadingCache = temporaryLoadingCache;
  }

  @Override
  public void clear() {
    loadingCache.invalidateAll();
    temporaryLoadingCache.invalidateAll();
  }

  @Override
  public Serializable _get(String key) {
    if (StrUtil.isBlank(key)) {
      return null;
    }
    Serializable ret = loadingCache.getIfPresent(key);
    if (ret == null) {
      ret = temporaryLoadingCache.getIfPresent(key);
    }

    return ret;
  }

  @Override
  public Collection<String> keys() {
    ConcurrentMap<String, Serializable> map = loadingCache.asMap();
    return map.keySet();
  }
  
  @Override
  public Collection<String> keysCollection() {
    return  loadingCache.asMap().keySet();
  }

  @Override
  public void put(String key, Serializable value) {
    if (StrUtil.isBlank(key)) {
      return;
    }
    loadingCache.put(key, value);
  }

  @Override
  public void putTemporary(String key, Serializable value) {
    if (StrUtil.isBlank(key)) {
      return;
    }
    temporaryLoadingCache.put(key, value);
  }

  @Override
  public void remove(String key) {
    if (StrUtil.isBlank(key)) {
      return;
    }
    loadingCache.invalidate(key);
    temporaryLoadingCache.invalidate(key);
  }

  /**
   * 
   * @return
   * @author: tanyaowu
   */
  public ConcurrentMap<String, Serializable> asMap() {
    return loadingCache.asMap();
  }

  /**
   * 
   * @return
   * @author: tanyaowu
   */
  public long size() {
    return loadingCache.size();
  }

  @Override
  public long ttl(String key) {
    throw new RuntimeException("不支持ttl");
  }


}
