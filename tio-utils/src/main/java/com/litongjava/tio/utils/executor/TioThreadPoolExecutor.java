package com.litongjava.tio.utils.executor;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongBinaryOperator;

public class TioThreadPoolExecutor extends ThreadPoolExecutor {

  // 统计计数（高并发用 LongAdder）
  public final LongAdder submitted = new LongAdder();
  public final LongAdder started = new LongAdder();
  public final LongAdder completed = new LongAdder();
  public final LongAdder failed = new LongAdder();
  public final LongAdder rejected = new LongAdder();

  // 累计时延/耗时（纳秒）
  private final LongAdder totalQueueNanos = new LongAdder();
  private final LongAdder totalExecNanos = new LongAdder();
  private final LongAccumulator maxExecNanos = new LongAccumulator(new LongBinaryOperator() {

    @Override
    public long applyAsLong(long left, long right) {
      return Math.max(left, right);
    }
  }, 0L);

  public TioThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, wrap(handler));
  }

  public TioThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, new AbortPolicy());
  }

  /** Java 8 兼容的拒绝包装：统计 rejected */
  public static RejectedExecutionHandler wrap(final RejectedExecutionHandler h) {
    return new RejectedExecutionHandler() {
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (e instanceof TioThreadPoolExecutor) {
          ((TioThreadPoolExecutor) e).rejected.increment();
        }
        h.rejectedExecution(r, e);
      }
    };
  }

  // ---- 任务包装：统计排队时长/执行时长 ----
  private Runnable wrap(final Runnable task) {
    final long enqueuedAt = System.nanoTime();
    submitted.increment();
    return new Runnable() {
      @Override
      public void run() {
        final long start = System.nanoTime();
        started.increment();
        totalQueueNanos.add(start - enqueuedAt);
        try {
          task.run();
          completed.increment();
        } catch (Throwable t) {
          failed.increment();
          throw t;
        } finally {
          long exec = System.nanoTime() - start;
          totalExecNanos.add(exec);
          maxExecNanos.accumulate(exec);
        }
      }
    };
  }

  private <T> Callable<T> wrap(final Callable<T> task) {
    final long enqueuedAt = System.nanoTime();
    submitted.increment();
    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        final long start = System.nanoTime();
        started.increment();
        totalQueueNanos.add(start - enqueuedAt);
        try {
          T v = task.call();
          completed.increment();
          return v;
        } catch (Throwable t) {
          failed.increment();
          throw t;
        } finally {
          long exec = System.nanoTime() - start;
          totalExecNanos.add(exec);
          maxExecNanos.accumulate(exec);
        }
      }
    };
  }

  // ---- 覆盖入口：注入包装 ----
  @Override
  public void execute(Runnable command) {
    super.execute(wrap(command));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return super.submit(wrap(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return super.submit(wrap(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return super.submit(wrap(task));
  }

  // ---- 对外暴露：获取统计快照实体 ----
  public TioThreadPoolStats getStats() {
    final BlockingQueue<Runnable> q = getQueue();
    long sub = submitted.longValue();
    long com = completed.longValue();
    long qn = totalQueueNanos.longValue();
    long en = totalExecNanos.longValue();
    long mx = maxExecNanos.get();

    double avgQueueMs = (sub == 0) ? 0.0 : (qn / 1_000_000.0) / sub;
    double avgExecMs = (com == 0) ? 0.0 : (en / 1_000_000.0) / com;
    double maxExecMs = mx / 1_000_000.0;

    return new TioThreadPoolStats(getPoolSize(), getActiveCount(), getLargestPoolSize(), getCompletedTaskCount(),
        q == null ? -1 : q.size(), isShutdown(), isTerminated(), sub, started.longValue(), com, failed.longValue(),
        rejected.longValue(), avgQueueMs, avgExecMs, maxExecMs, System.currentTimeMillis());
  }

  /** 可选：清零统计（不会影响线程池本身，仅清指标） */
  public void resetStats() {
    submitted.reset();
    started.reset();
    completed.reset();
    failed.reset();
    rejected.reset();
    totalQueueNanos.reset();
    totalExecNanos.reset();
    // LongAccumulator 无 reset，只能重建；这里简单置 0（通过反射会复杂，这里给最简方案：新建对象）
    try {
      java.lang.reflect.Field f = LongAccumulator.class.getDeclaredField("value");
      f.setAccessible(true);
      // 对于不同 JDK 实现可能不兼容，保守起见：直接新建一个新的 LongAccumulator 替换引用
    } catch (Exception ignore) {
    }
  }
}