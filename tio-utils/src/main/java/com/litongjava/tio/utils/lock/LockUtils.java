package com.litongjava.tio.utils.lock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCache;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;

/**
 * 锁对象工具类
 */
public class LockUtils {
  private static Logger log = LoggerFactory.getLogger(LockUtils.class);
  private static final String LOCK_TYPE_OBJ = "OBJ";
  private static final String LOCK_TYPE_RW = "RW";
  private static final Object defaultLockObjForObj = new Object();
  private static final Object defaultLockObjForRw = new Object();
  // LOCAL_LOCKS
  private static final ConcurrentMapCache LOCAL_LOCKS = ConcurrentMapCacheFactory.INSTANCE
      .register(LockUtils.class.getName() + LOCK_TYPE_OBJ, null, 3600L);
  // LOCAL_READWRITE_LOCKS
  private static final ConcurrentMapCache LOCAL_READWRITE_LOCKS = ConcurrentMapCacheFactory.INSTANCE
      .register(LockUtils.class.getName() + LOCK_TYPE_RW, null, 3600L);

  /**
   * 获取锁对象，用于synchronized(lockObj)
   * 
   * @param key
   * @return
   * @author tanyaowu
   */
  public static Serializable getLockObj(String key) {

    return getLockObj(key, null);
  }

  /**
   * 获取锁对象，用于synchronized(lockObj)
   * 
   * @param key
   * @param myLock 获取LockObj的锁，可以为null
   * @return
   * @author tanyaowu
   */
  public static Serializable getLockObj(String key, Object myLock) {
    Serializable lock = LOCAL_LOCKS.get(key);
    if (lock == null) {
      Object ml = myLock;
      if (ml == null) {
        ml = defaultLockObjForObj;
      }
      synchronized (ml) {
        lock = LOCAL_LOCKS.get(key);
        if (lock == null) {
          lock = new Serializable() {
            private static final long serialVersionUID = 255956860617836425L;
          };
          LOCAL_LOCKS.put(key, lock);
        }
      }
    }
    return lock;
  }

  /**
   * 获取读写锁
   * 
   * @param key
   * @param myLock 获取ReentrantReadWriteLock的锁，可以为null
   * @return
   * @author tanyaowu
   */
  public static ReentrantReadWriteLock getReentrantReadWriteLock(String key, Object myLock) {
    ReentrantReadWriteLock lock = (ReentrantReadWriteLock) LOCAL_READWRITE_LOCKS.get(key);
    if (lock == null) {
      Object ml = myLock;
      if (ml == null) {
        ml = defaultLockObjForRw;
      }
      synchronized (ml) {
        lock = (ReentrantReadWriteLock) LOCAL_READWRITE_LOCKS.get(key);
        if (lock == null) {
          lock = new ReentrantReadWriteLock();
          LOCAL_READWRITE_LOCKS.put(key, lock);
        }
      }
    }
    return lock;
  }

  /**
   * 用读写锁操作<br>
   * 1、能拿到写锁的线程会执行readWriteLockHandler.write()<br>
   * 2、没拿到写锁的线程，会等待获取读锁，注：获取到读锁的线程，什么也不会执行<br>
   * 3、当一段代码只允许被一个线程执行时，才用本函数，不要理解成同步等待了<br>
   * <br>
   * <strong>注意：对于一些需要判断null等其它条件才执行的操作，在write()方法中建议再检查一次，这个跟double
   * check的原理是一样的</strong><br>
   * 
   * @param key
   * @param myLock               获取ReentrantReadWriteLock的锁，可以为null
   * @param readWriteLockHandler 小心：该对象的write()方法并不一定会被执行
   * @throws Exception
   */
  public static void runWriteOrWaitRead(String key, Object myLock, ReadWriteLockHandler readWriteLockHandler)
      throws Exception {
    runWriteOrWaitRead(key, myLock, readWriteLockHandler, 180L);
  }

  /**
   * 运行write或者等待读锁<br>
   * 1、能拿到写锁的线程会执行readWriteLockHandler.write()<br>
   * 2、没拿到写锁的线程，会等待获取读锁，注：获取到读锁的线程，什么也不会执行<br>
   * 3、当一段代码只允许被一个线程执行时，才用本函数，不要理解成同步等待了<br>
   * <br>
   * <strong>注意：对于一些需要判断null等其它条件才执行的操作，在write()方法中建议再检查一次，这个跟double
   * check的原理是一样的</strong><br>
   * 
   * @param key
   * @param myLock               获取ReentrantReadWriteLock的锁，可以为null
   * @param readWriteLockHandler 小心：该对象的write()方法并不一定会被执行
   * @param readWaitTimeInSecond 没拿到写锁的线程，等读锁的时间，单位：秒
   * @return
   * @throws Exception
   * @author tanyaowu
   */
  public static void runWriteOrWaitRead(String key, Object myLock, ReadWriteLockHandler readWriteLockHandler,
      Long readWaitTimeInSecond) throws Exception {
    ReentrantReadWriteLock rwLock = getReentrantReadWriteLock(key, myLock);
//		ReadWriteRet ret = new ReadWriteRet();
    WriteLock writeLock = rwLock.writeLock();
    boolean tryWrite = writeLock.tryLock();
    if (tryWrite) {
      try {
        readWriteLockHandler.write();
//				ret.writeRet = writeRet;
      } finally {
//				ret.isWriteRunned = true;
        writeLock.unlock();
      }
    } else {
      ReadLock readLock = rwLock.readLock();
      boolean tryRead = false;
      try {
        tryRead = readLock.tryLock(readWaitTimeInSecond, TimeUnit.SECONDS);
        if (tryRead) {
//					try {
//						readWriteLockHandler.read();
//						ret.readRet = readRet;
//					} finally {
//						ret.isReadRunned = true;
          readLock.unlock();
//					}
        }
      } catch (InterruptedException e) {
        log.error(e.toString(), e);
      }
    }
//		return ret;
  }
}
