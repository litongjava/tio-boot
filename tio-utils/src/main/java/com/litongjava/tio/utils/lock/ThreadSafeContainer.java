package com.litongjava.tio.utils.lock;

/**
 * 标记接口：表示该对象内部使用线程安全的数据结构 用于告诉 ObjWithLock 不需要额外加锁
 */
public interface ThreadSafeContainer {
  // 空接口，仅用于标记
}