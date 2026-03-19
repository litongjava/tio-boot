package com.litongjava.tio.utils.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 高性能版本 - 内部使用 ConcurrentHashMap 实现 ThreadSafeContainer 标记接口，告诉 ObjWithLock
 * 不需要加锁
 */
public class MapWithLock<K, V> extends ObjWithLock<Map<K, V>> implements ThreadSafeContainer {
  private static final long serialVersionUID = -652862323697152866L;
  private static final Logger log = LoggerFactory.getLogger(MapWithLock.class);

  public MapWithLock() {
    super(new ConcurrentHashMap<>());
  }

  public MapWithLock(int initCapacity) {
    super(new ConcurrentHashMap<>(initCapacity));
  }

  public MapWithLock(Map<K, V> map) {
    super(new ConcurrentHashMap<>());
    if (map != null && !map.isEmpty()) {
      this.getObj().putAll(map);
    }
  }

  public MapWithLock(Map<K, V> map, java.util.concurrent.locks.ReentrantReadWriteLock lock) {
    this(map);
  }

  public V put(K key, V value) {
    try {
      Map<K, V> map = this.getObj();
      return map.put(key, value);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public V putIfAbsent(K key, V value) {
    try {
      Map<K, V> map = this.getObj();
      return map.putIfAbsent(key, value);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public void putAll(Map<K, V> otherMap) {
    if (otherMap == null || otherMap.isEmpty()) {
      return;
    }
    try {
      Map<K, V> map = this.getObj();
      map.putAll(otherMap);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  public V remove(K key) {
    try {
      Map<K, V> map = this.getObj();
      return map.remove(key);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public void clear() {
    try {
      Map<K, V> map = this.getObj();
      map.clear();
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  public V get(K key) {
    try {
      Map<K, V> map = this.getObj();
      return map.get(key);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public int size() {
    Map<K, V> map = this.getObj();
    return map.size();
  }

  public Map<K, V> copy() {
    try {
      Map<K, V> map = this.getObj();
      if (map.size() > 0) {
        return new ConcurrentHashMap<>(map);
      }
      return null;
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }
}