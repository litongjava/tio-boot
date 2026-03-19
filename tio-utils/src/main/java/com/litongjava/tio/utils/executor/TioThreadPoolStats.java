package com.litongjava.tio.utils.executor;

import java.io.Serializable;

public final class TioThreadPoolStats implements Serializable {
  private static final long serialVersionUID = 1L;

  // ThreadPoolExecutor 原生指标
  private final int poolSize;
  private final int activeCount;
  private final int largestPoolSize;
  private final long completedTaskCount;
  private final int queueSize;
  private final boolean shutdown;
  private final boolean terminated;

  // 统计指标
  private final long submitted;
  private final long started;
  private final long completed;
  private final long failed;
  private final long rejected;

  // 延迟/耗时（毫秒）
  private final double avgQueueMs;
  private final double avgExecMs;
  private final double maxExecMs;

  private final long timestampMs;

  public TioThreadPoolStats(int poolSize, int activeCount, int largestPoolSize, long completedTaskCount, int queueSize,
      boolean shutdown, boolean terminated, long submitted, long started, long completed, long failed, long rejected,
      double avgQueueMs, double avgExecMs, double maxExecMs, long timestampMs) {
    this.poolSize = poolSize;
    this.activeCount = activeCount;
    this.largestPoolSize = largestPoolSize;
    this.completedTaskCount = completedTaskCount;
    this.queueSize = queueSize;
    this.shutdown = shutdown;
    this.terminated = terminated;
    this.submitted = submitted;
    this.started = started;
    this.completed = completed;
    this.failed = failed;
    this.rejected = rejected;
    this.avgQueueMs = avgQueueMs;
    this.avgExecMs = avgExecMs;
    this.maxExecMs = maxExecMs;
    this.timestampMs = timestampMs;
  }

  public int getPoolSize() {
    return poolSize;
  }

  public int getActiveCount() {
    return activeCount;
  }

  public int getLargestPoolSize() {
    return largestPoolSize;
  }

  public long getCompletedTaskCount() {
    return completedTaskCount;
  }

  public int getQueueSize() {
    return queueSize;
  }

  public boolean isShutdown() {
    return shutdown;
  }

  public boolean isTerminated() {
    return terminated;
  }

  public long getSubmitted() {
    return submitted;
  }

  public long getStarted() {
    return started;
  }

  public long getCompleted() {
    return completed;
  }

  public long getFailed() {
    return failed;
  }

  public long getRejected() {
    return rejected;
  }

  public double getAvgQueueMs() {
    return avgQueueMs;
  }

  public double getAvgExecMs() {
    return avgExecMs;
  }

  public double getMaxExecMs() {
    return maxExecMs;
  }

  public long getTimestampMs() {
    return timestampMs;
  }

  @Override
  public String toString() {
    return "TioThreadPoolStats{" + "poolSize=" + poolSize + ", activeCount=" + activeCount + ", largestPoolSize="
        + largestPoolSize + ", completedTaskCount=" + completedTaskCount + ", queueSize=" + queueSize + ", shutdown="
        + shutdown + ", terminated=" + terminated + ", submitted=" + submitted + ", started=" + started + ", completed="
        + completed + ", failed=" + failed + ", rejected=" + rejected + ", avgQueueMs=" + avgQueueMs + ", avgExecMs="
        + avgExecMs + ", maxExecMs=" + maxExecMs + ", timestampMs=" + timestampMs + '}';
  }
}
