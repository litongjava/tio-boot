package com.litongjava.tio.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.thread.pool.DefaultThreadFactory;
import com.litongjava.tio.utils.thread.pool.SynThreadPoolExecutor;
import com.litongjava.tio.utils.thread.pool.TioCallerRunsPolicy;

/**
 * This class provides utility methods for managing thread pools.
 * <p>
 * Note: The groupExecutor is deprecated and is no longer used as the default
 * TioServer thread.
 * </p>
 * 
 */
public class Threads {
  private static final Logger log = LoggerFactory.getLogger(Threads.class);
  public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  public static final int CORE_POOL_SIZE = AVAILABLE_PROCESSORS;
  public static final int MAX_POOL_SIZE_FOR_TIO = Integer.getInteger("TIO_MAX_POOL_SIZE_FOR_TIO", Math.max(CORE_POOL_SIZE * 3, 64));
  public static final int MAX_POOL_SIZE_FOR_GROUP = Integer.getInteger("TIO_MAX_POOL_SIZE_FOR_GROUP", Math.max(CORE_POOL_SIZE * 16, 256));
  public static final long KEEP_ALIVE_TIME = 0L;
  public static final String GROUP_THREAD_NAME = "tio-group";
  public static final String WORKER_THREAD_NAME = "tio-worker";
  // private static final int QUEUE_CAPACITY = 1000000;
  private static ExecutorService readExecutor = null;
  private static SynThreadPoolExecutor tioExecutor = null;

  /**
   * Returns the group executor. If it does not exist, a new one will be created.
   *
   * @return The group executor.
   */
  public static ExecutorService getReadExecutor() {
    if (readExecutor != null) {
      return readExecutor;
    }

    synchronized (Threads.class) {
      if (readExecutor == null) {
        readExecutor = newReadExecutor();
        //log.info("new group thead pool:{}", groupExecutor);
      }
    }
    return readExecutor;
  }

  /**
   * Creates and returns a new group executor.
   *
   * @return The newly created group executor.
   */
//  private static ThreadPoolExecutor newGroupExecutor() {
//    LinkedBlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();
//    DefaultThreadFactory threadFactory = DefaultThreadFactory.getInstance(GROUP_THREAD_NAME, Thread.MAX_PRIORITY);
//    CallerRunsPolicy callerRunsPolicy = new TioCallerRunsPolicy();
//    ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_POOL_SIZE_FOR_GROUP, MAX_POOL_SIZE_FOR_GROUP, KEEP_ALIVE_TIME,
//        //
//        TimeUnit.SECONDS, runnableQueue, threadFactory, callerRunsPolicy);
//    executor.prestartCoreThread();
//    
// 
//    return executor;
//  }
  
  private static ExecutorService newReadExecutor() {
    return Executors.newCachedThreadPool(DefaultThreadFactory.getInstance(GROUP_THREAD_NAME, Thread.MAX_PRIORITY));
  }


  /**
   * Returns the Tio executor. If it does not exist, a new one will be created.
   *
   * @return The Tio executor.
   */
  public static SynThreadPoolExecutor getTioExecutor() {
    if (tioExecutor != null) {
      return tioExecutor;
    }

    synchronized (Threads.class) {
      if (tioExecutor == null) {
        tioExecutor = newTioExecutor();
        log.info("new worker thead pool:{}", tioExecutor);
      }
    }
    return tioExecutor;
  }

  /**
   * Creates and returns a new Tio executor.
   *
   * @return The newly created Tio executor.
   */
  private static SynThreadPoolExecutor newTioExecutor() {
    LinkedBlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();
    DefaultThreadFactory defaultThreadFactory = DefaultThreadFactory.getInstance(WORKER_THREAD_NAME, Thread.MAX_PRIORITY);
    CallerRunsPolicy callerRunsPolicy = new TioCallerRunsPolicy();
    SynThreadPoolExecutor executor = new SynThreadPoolExecutor(MAX_POOL_SIZE_FOR_TIO, MAX_POOL_SIZE_FOR_TIO, KEEP_ALIVE_TIME,
        //
        runnableQueue, defaultThreadFactory,
        //
        WORKER_THREAD_NAME, callerRunsPolicy);
    executor.prestartCoreThread();
    return executor;
  }

  /**
   * Returns the status of the thread pools.
   *
   * @return The status of the thread pools as a string buffer.
   */
  public static StringBuffer status() {
    StringBuffer stringBuffer = new StringBuffer();
    //stringBuffer.append(printThreadPoolStatus(groupExecutor, GROUP_THREAD_NAME));
    stringBuffer.append(printThreadPoolStatus(tioExecutor, WORKER_THREAD_NAME));
    return stringBuffer;
  }

  /**
   * Prints the status of the specified thread pool.
   *
   * @param executor The thread pool executor.
   * @param poolName The name of the thread pool.
   * @return The status of the thread pool as a string buffer.
   */
  public static StringBuffer printThreadPoolStatus(ThreadPoolExecutor executor, String poolName) {
    if (executor == null) {
      return new StringBuffer("Thread pool " + poolName + " is not initialized.\n");
    }

    int corePoolSize = executor.getCorePoolSize();
    int maximumPoolSize = executor.getMaximumPoolSize();
    int poolSize = executor.getPoolSize();
    int activeCount = executor.getActiveCount();
    int queueSize = executor.getQueue().size();
    long taskCount = executor.getTaskCount();
    long completedTaskCount = executor.getCompletedTaskCount();
    RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
    int remainingCapacity = executor.getQueue().remainingCapacity();

    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("Thread Pool Name: " + poolName).append("\n");
    stringBuffer.append("Core Pool Size: " + corePoolSize).append("\n");
    stringBuffer.append("Maximum Pool Size: " + maximumPoolSize).append("\n");
    stringBuffer.append("Current Pool Size: " + poolSize).append("\n");
    stringBuffer.append("Active Thread Count: " + activeCount).append("\n");
    stringBuffer.append("Tasks in Queue: " + queueSize).append("\n");
    stringBuffer.append("Total Task Count: " + taskCount).append("\n");
    stringBuffer.append("Completed Task Count: " + completedTaskCount).append("\n");
    stringBuffer.append("Current Rejection Policy: " + handler.getClass().getSimpleName()).append("\n");
    stringBuffer.append("Queue Remaining Capacity: " + remainingCapacity).append("\n");
    return stringBuffer;
  }

  /**
   * Shuts down the executors and releases resources.
   */
  public static boolean close() {
    boolean ret = true;
    if (readExecutor != null) {
      readExecutor.shutdown();
      try {
        ret = readExecutor.awaitTermination(6000, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      log.info("shutdown group thead pool:{}", readExecutor);
      readExecutor = null;
    }

    if (tioExecutor != null) {
      tioExecutor.shutdown();
      try {
        ret = tioExecutor.awaitTermination(6000, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      log.info("shutdown worker thead pool:{}", tioExecutor);
      tioExecutor = null;
    }
    return ret;
  }

  // Private constructor to prevent instantiation
  private Threads() {
  }
}
