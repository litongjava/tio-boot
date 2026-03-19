package com.litongjava.tio.core.pool;

/** 统计快照结构 */
public class ByteBufferPoolMetricsSnapshot {
  public final long reuseHit;
  public final long reuseMissSize;
  public final long reuseMissType;
  public final long queuePoll;
  public final long queueOffer;
  public final long newAlloc;
  public final long cleanCount;
  public final long tryCleanRuns;
  public final long tryCleanDrains;
  public final long allocOps;
  public final long allocTimeNs;
  public final long cleanOps;
  public final long cleanTimeNs;

  public ByteBufferPoolMetricsSnapshot(long reuseHit, long reuseMissSize, long reuseMissType, long queuePoll, long queueOffer,
      long newAlloc, long cleanCount, long tryCleanRuns, long tryCleanDrains, long allocOps, long allocTimeNs,
      long cleanOps, long cleanTimeNs) {
    this.reuseHit = reuseHit;
    this.reuseMissSize = reuseMissSize;
    this.reuseMissType = reuseMissType;
    this.queuePoll = queuePoll;
    this.queueOffer = queueOffer;
    this.newAlloc = newAlloc;
    this.cleanCount = cleanCount;
    this.tryCleanRuns = tryCleanRuns;
    this.tryCleanDrains = tryCleanDrains;
    this.allocOps = allocOps;
    this.allocTimeNs = allocTimeNs;
    this.cleanOps = cleanOps;
    this.cleanTimeNs = cleanTimeNs;
  }

  @Override
  public String toString() {
    long avgAllocNs = (allocOps == 0) ? 0 : (allocTimeNs / Math.max(1, allocOps));
    long avgCleanNs = (cleanOps == 0) ? 0 : (cleanTimeNs / Math.max(1, cleanOps));
    return new StringBuilder(256).append("reuseHit=").append(reuseHit).append(", reuseMissSize=").append(reuseMissSize)
        .append(", reuseMissType=").append(reuseMissType).append(", queuePoll=").append(queuePoll)
        .append(", queueOffer=").append(queueOffer).append(", newAlloc=").append(newAlloc).append(", cleanCount=")
        .append(cleanCount).append(", tryCleanRuns=").append(tryCleanRuns).append(", tryCleanDrains=")
        .append(tryCleanDrains).append(", allocOps=").append(allocOps).append(", avgAllocNs=").append(avgAllocNs)
        .append(", cleanOps=").append(cleanOps).append(", avgCleanNs=").append(avgCleanNs).toString();
  }
}
