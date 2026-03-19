package com.litongjava.tio.utils.lock;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 高性能版本 - 内部使用 ConcurrentHashMap.newKeySet() 实现 ThreadSafeContainer 标记接口，告诉
 * ObjWithLock 不需要加锁
 */
public class SetWithLock<T> extends ObjWithLock<Set<T>> implements ThreadSafeContainer {
  private static final long serialVersionUID = -2305909960649321346L;
  private static final Logger log = LoggerFactory.getLogger(SetWithLock.class);

  public SetWithLock(Set<T> set) {
    super(ConcurrentHashMap.newKeySet());
    if (set != null && !set.isEmpty()) {
      this.getObj().addAll(set);
    }
  }

  public SetWithLock(Set<T> set, java.util.concurrent.locks.ReentrantReadWriteLock lock) {
    this(set);
  }

  public boolean add(T t) {
    try {
      Set<T> set = this.getObj();
      return set.add(t);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return false;
    }
  }

  public void clear() {
    try {
      Set<T> set = this.getObj();
      set.clear();
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  public boolean remove(T t) {
    try {
      Set<T> set = this.getObj();
      return set.remove(t);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
      return false;
    }
  }

  public int size() {
    Set<T> set = this.getObj();
    return set.size();
  }
}