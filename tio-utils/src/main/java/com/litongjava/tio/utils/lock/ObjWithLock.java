package com.litongjava.tio.utils.lock;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjWithLock<T> implements Serializable {
  private static final long serialVersionUID = -3048283373239453901L;
  private static Logger log = LoggerFactory.getLogger(ObjWithLock.class);

  private T obj = null;
  private ReentrantReadWriteLock lock = null;
  private final boolean needLock;

  public ObjWithLock(T obj) {
    this(obj, true);
  }

  public ObjWithLock(T obj, boolean needLock) {
    this.obj = obj;
    this.needLock = needLock;
    this.lock = null;
  }

  public ObjWithLock(T obj, ReentrantReadWriteLock lock) {
    this.obj = obj;
    this.lock = lock;
    this.needLock = true;
  }

  public ReentrantReadWriteLock getLock() {
    if (!needLock) {
      return null;
    }
    if (lock == null) {
      synchronized (this) {
        if (lock == null) {
          lock = new ReentrantReadWriteLock();
        }
      }
    }
    return lock;
  }

  public WriteLock writeLock() {
    ReentrantReadWriteLock l = getLock();
    return l != null ? l.writeLock() : null;
  }

  public ReadLock readLock() {
    ReentrantReadWriteLock l = getLock();
    return l != null ? l.readLock() : null;
  }

  public T getObj() {
    return obj;
  }

  public void setObj(T obj) {
    this.obj = obj;
  }

  public void handle(ReadLockHandler<T> readLockHandler) {
    if (!needLock) {
      try {
        readLockHandler.handler(obj);
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      }
      return;
    }

    ReadLock readLock = readLock();
    if (readLock == null) {
      try {
        readLockHandler.handler(obj);
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      }
      return;
    }

    readLock.lock();
    try {
      readLockHandler.handler(obj);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    } finally {
      readLock.unlock();
    }
  }

  public void handle(WriteLockHandler<T> writeLockHandler) {
    if (!needLock) {
      try {
        writeLockHandler.handler(obj);
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      }
      return;
    }

    WriteLock writeLock = writeLock();
    if (writeLock == null) {
      try {
        writeLockHandler.handler(obj);
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      }
      return;
    }

    writeLock.lock();
    try {
      writeLockHandler.handler(obj);
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    } finally {
      writeLock.unlock();
    }
  }
}