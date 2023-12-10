package com.litongjava.tio.boot.executor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import com.litongjava.tio.utils.thread.pool.DefaultThreadFactory;
import com.litongjava.tio.utils.thread.pool.SynThreadPoolExecutor;
import com.litongjava.tio.utils.thread.pool.TioCallerRunsPolicy;

/**
 *
 * @author litongjava
 * 2023-10-16 03:13:40
 */
public class Threads {
  public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  public static final int CORE_POOL_SIZE = AVAILABLE_PROCESSORS * 1;
  public static final int MAX_POOL_SIZE_FOR_TIO = Integer.getInteger("TIO_MAX_POOL_SIZE_FOR_TIO",
      Math.max(CORE_POOL_SIZE * 3, 64));
  public static final int MAX_POOL_SIZE_FOR_GROUP = Integer.getInteger("TIO_MAX_POOL_SIZE_FOR_GROUP",
      Math.max(CORE_POOL_SIZE * 16, 256));
  public static final long KEEP_ALIVE_TIME = 0L; // 360000L;
  @SuppressWarnings("unused")
  private static final int QUEUE_CAPACITY = 1000000;
  private static ThreadPoolExecutor groupExecutor = null;
  private static SynThreadPoolExecutor tioExecutor = null;

  /**
   * 
   * @return
   * @author tanyaowu
   */
  public static ThreadPoolExecutor getGroupExecutor() {
    if (groupExecutor != null) {
      return groupExecutor;
    }

    return newGruopExecutor();
  }

  public static ThreadPoolExecutor newGruopExecutor() {
    synchronized (Threads.class) {
      LinkedBlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();
      // ArrayBlockingQueue<Runnable> groupQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
      String threadName = "tio-group";
      DefaultThreadFactory threadFactory = DefaultThreadFactory.getInstance(threadName, Thread.MAX_PRIORITY);
      CallerRunsPolicy callerRunsPolicy = new TioCallerRunsPolicy();
      groupExecutor = new ThreadPoolExecutor(MAX_POOL_SIZE_FOR_GROUP, MAX_POOL_SIZE_FOR_GROUP, KEEP_ALIVE_TIME,
          TimeUnit.SECONDS, runnableQueue, threadFactory, callerRunsPolicy);
      // groupExecutor = new ThreadPoolExecutor(AVAILABLE_PROCESSORS * 2, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), defaultThreadFactory);

      groupExecutor.prestartCoreThread();
      // groupExecutor.prestartAllCoreThreads();
      return groupExecutor;
    }
  }

  /**
   * 
   * @return
   * @author tanyaowu
   */
  public static SynThreadPoolExecutor getTioExecutor() {
    if (tioExecutor != null) {
      return tioExecutor;
    }

    return newTioExecutor();
  }

  public static SynThreadPoolExecutor newTioExecutor() {
    synchronized (Threads.class) {
      LinkedBlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();
      // ArrayBlockingQueue<Runnable> tioQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
      String threadName = "tio-worker";
      DefaultThreadFactory defaultThreadFactory = DefaultThreadFactory.getInstance(threadName, Thread.MAX_PRIORITY);
      CallerRunsPolicy callerRunsPolicy = new TioCallerRunsPolicy();
      tioExecutor = new SynThreadPoolExecutor(MAX_POOL_SIZE_FOR_TIO, MAX_POOL_SIZE_FOR_TIO, KEEP_ALIVE_TIME,
          runnableQueue, defaultThreadFactory, threadName, callerRunsPolicy);
      // tioExecutor = new SynThreadPoolExecutor(AVAILABLE_PROCESSORS * 2, Integer.MAX_VALUE, 60, new SynchronousQueue<Runnable>(), defaultThreadFactory, tioThreadName);

      tioExecutor.prestartCoreThread();
      // tioExecutor.prestartAllCoreThreads();
      return tioExecutor;
    }
  }

  public static void close() {
    groupExecutor.shutdown();
    tioExecutor.shutdown();
    groupExecutor = null;
    tioExecutor = null;
  }

  /**
   *
   */
  private Threads() {
  }
}
