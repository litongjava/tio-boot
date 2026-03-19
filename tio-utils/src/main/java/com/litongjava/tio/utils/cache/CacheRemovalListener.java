package com.litongjava.tio.utils.cache;

@FunctionalInterface
public interface CacheRemovalListener<K, V> {
  void onCacheRemoval(K key, V value, RemovalCause cause);
}
