package nexus.io.tio.utils.lock;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 锁对象工具类
 */
public class LockUtils {
  private static Logger log = LoggerFactory.getLogger(LockUtils.class);
  private static final LockRegistry<Serializable> LOCAL_LOCKS = new LockRegistry<>();
  private static final LockRegistry<ReentrantReadWriteLock> LOCAL_READWRITE_LOCKS = new LockRegistry<>();

  private static final class LockReference<T> extends WeakReference<T> {
    private final String key;
    LockReference(String key, T value, ReferenceQueue<T> queue) {
      super(value, queue);
      this.key = key;
    }
  }

  private static final class LockRegistry<T> {
    private final Map<String, LockReference<T>> locks = new HashMap<>();
    private final ReferenceQueue<T> queue = new ReferenceQueue<>();

    synchronized T get(String key, Supplier<T> factory) {
      Objects.requireNonNull(key, "Lock key");
      LockReference<?> expired;
      while ((expired = (LockReference<?>) queue.poll()) != null) {
        locks.remove(expired.key, expired);
      }
      LockReference<T> reference = locks.get(key);
      T lock = reference == null ? null : reference.get();
      if (lock == null) {
        lock = factory.get();
        locks.put(key, new LockReference<>(key, lock, queue));
      }
      return lock;
    }
  }

  private static final class RetainedReadWriteLock extends ReentrantReadWriteLock {
    private static final long serialVersionUID = 1L;
    private final ReadLock retainedRead = new RetainedReadLock(this);
    private final WriteLock retainedWrite = new RetainedWriteLock(this);
    @Override public ReadLock readLock() { return retainedRead; }
    @Override public WriteLock writeLock() { return retainedWrite; }
  }

  private static final class RetainedReadLock extends ReadLock {
    private static final long serialVersionUID = 1L;
    // A retained handle must keep its owning registry value alive.
    @SuppressWarnings("unused") private final ReentrantReadWriteLock owner;
    RetainedReadLock(ReentrantReadWriteLock owner) { super(owner); this.owner = owner; }
  }

  private static final class RetainedWriteLock extends WriteLock {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused") private final ReentrantReadWriteLock owner;
    RetainedWriteLock(ReentrantReadWriteLock owner) { super(owner); this.owner = owner; }
  }

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
   * @param myLock retained for source compatibility; registry synchronization is internal
   * @return
   * @author tanyaowu
   */
  public static Serializable getLockObj(String key, Object myLock) {
    return LOCAL_LOCKS.get(key, () -> new Serializable() {
      private static final long serialVersionUID = 255956860617836425L;
    });
  }

  /**
   * 获取读写锁
   * 
   * @param key
   * @param myLock retained for source compatibility; registry synchronization is internal
   * @return
   * @author tanyaowu
   */
  public static ReentrantReadWriteLock getReentrantReadWriteLock(String key, Object myLock) {
    return LOCAL_READWRITE_LOCKS.get(key, RetainedReadWriteLock::new);
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
        Thread.currentThread().interrupt();
        log.error(e.toString(), e);
      }
    }
//		return ret;
  }
}
