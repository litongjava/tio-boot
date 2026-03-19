package com.litongjava.tio.core.pool;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import com.litongjava.enhance.buffer.BufferMemoryStat;
import com.litongjava.enhance.buffer.BufferMomeryInfo;
import com.litongjava.enhance.buffer.BufferPagePool;
import com.litongjava.enhance.buffer.DirectBufferCleaner;
import com.litongjava.enhance.buffer.GlobalScheduler;
import com.litongjava.enhance.buffer.VirtualBuffer;
import com.litongjava.tio.core.TioConfig;

public class BufferPoolUtils {
  /** 是否使用直接内存缓冲区（可通过环境变量开关） */
  public static final boolean direct = TioConfig.direct;

  /** 底层页池 */
  public static BufferPagePool bufferPool = new BufferPagePool(1, direct);

  /** “系统是否空闲”的标记，由分配与清理线程共享，需保障可见性 */
  private static volatile boolean idle = true;

  /** 待复用/待清理的缓冲区队列（简单实现用一条队列） */
  private static final ConcurrentLinkedQueue<ByteBuffer> cleanBuffers = new ConcurrentLinkedQueue<>();

  static {
    // 初始延迟 500ms，之后每 1000ms 执行一次
    GlobalScheduler.scheduleWithFixedDelay(BufferPoolUtils::tryClean, 500, 1000, TimeUnit.MILLISECONDS);
  }

  /**
   * 尝试清理：采用“两次空闲”策略。 第一次检测到非空闲 -> 置空闲并返回； 下一周期若仍为空闲 -> 逐步清理（每轮最多 10 个，避免抖动）。
   */
  public static void tryClean() {
    // 若上个周期内发生过分配（idle=false），本周期仅将其置回 true，不清理
    if (!idle) {
      idle = true;
      return;
    }
    // 已连续空闲 -> 执行有限度清理
    int count = 0;
    ByteBuffer buf;
    while (idle && count++ < 10 && (buf = cleanBuffers.poll()) != null) {
      clean0(buf);
    }
  }

  /** 实际释放直接缓冲区（堆缓冲区无需显式清理） */
  private static void clean0(ByteBuffer buffer) {
    if (buffer != null) {
      statCleanCount.increment();
      if (buffer.isDirect()) {
        try {
          DirectBufferCleaner.clean(buffer);
        } catch (Throwable e) {
          // 释放失败不影响后续逻辑
          e.printStackTrace();
        }
      }
    }
  }

  /** 释放队列中所有缓冲区资源（用于停止/卸载阶段） */
  public static void release() {
    ByteBuffer temp;
    while ((temp = cleanBuffers.poll()) != null) {
      clean0(temp);
    }
  }

  /*
   * ====================== VirtualBuffer 分配 ======================
   */

  /**
   * 按线程维度分配 Request 用 VirtualBuffer
   */
  public static VirtualBuffer allocateRequest(Integer size) {
    return bufferPool.allocateRequestByThreadId(size);
  }

  /**
   * 按线程维度分配 Response 用 VirtualBuffer
   **/
  public static VirtualBuffer allocateResponse(Integer size) {
    return bufferPool.allocateResponseByThreadId(size);
  }

  /**
   * 对齐到 chunkSize 的 Request 分配
   */
  public static VirtualBuffer allocateRequest(int chunkSize, int size) {
    if (size <= 0 || chunkSize <= 0) {
      throw new IllegalArgumentException("invalid size or chunkSize");
    }
    int m = size % chunkSize;
    long need = (m == 0) ? (long) size : (long) size + chunkSize - m;
    if (need > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("aligned size too large");
    }
    return allocateRequest((int) need);
  }

  /** 对齐到 chunkSize 的 Response 分配（补齐重载，方便对称使用） */
  public static VirtualBuffer allocateResponse(int chunkSize, int size) {
    if (size <= 0 || chunkSize <= 0) {
      throw new IllegalArgumentException("invalid size or chunkSize");
    }
    int m = size % chunkSize;
    long need = (m == 0) ? (long) size : (long) size + chunkSize - m;
    if (need > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("aligned size too large");
    }
    return allocateResponse((int) need);
  }

  private static final LongAdder statReuseHit = new LongAdder(); // 复用命中（容量+direct一致）
  private static final LongAdder statNewAlloc = new LongAdder(); // 新分配次数
  private static final LongAdder statCleanCount = new LongAdder(); // 实际清理次数

  /*
   * ====================== ByteBuffer 分配/回收 ======================
   */

  /**
   * 对齐到 chunkSize 的 Request 分配
   */
  public static ByteBuffer allocate(int chunkSize, int size) {
    if (size <= 0 || chunkSize <= 0) {
      throw new IllegalArgumentException("invalid size or chunkSize");
    }
    int m = size % chunkSize;
    long need = (m == 0) ? (long) size : (long) size + chunkSize - m;
    if (need > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("aligned size too large");
    }
    return allocate((int) need);
  }

  /**
   * 分配原生 ByteBuffer（走简单的复用与延迟清理策略）
   **/
  public static ByteBuffer allocate(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("size must be > 0");
    }
    // 有分配发生 -> 标记非空闲（让清理器下一轮先观察期）
    idle = false;

    // 优先尝试从复用队列取出匹配的缓冲区
    ByteBuffer bb = cleanBuffers.poll();

    if (bb != null) {
      boolean sizeMatch = (bb.capacity() == size);
      boolean typeMatch = (bb.isDirect() == direct);
      if (sizeMatch && typeMatch) {
        bb.clear(); // 复用：position=0, limit=capacity
        statReuseHit.increment();
        return bb;
      } else {
        clean0(bb);
      }
    }
    // 新分配
    statNewAlloc.increment();
    return direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);

  }

  /**
   * 归还一个缓冲区，进入延迟清理/复用队列
   */
  public static void clean(ByteBuffer cleanBuffer) {
    if (cleanBuffer == null) {
      return;
    }
    cleanBuffers.offer(cleanBuffer);
  }

  public static BufferMemoryStat getStat() {
    BufferMemoryStat memoryStat = new BufferMemoryStat();
    memoryStat.statNewAlloc = statNewAlloc.longValue();
    memoryStat.statCleanCount = statCleanCount.longValue();
    memoryStat.statReuseHit = statReuseHit.longValue();
    memoryStat.bufferSize = cleanBuffers.size();
    return memoryStat;
  }

  public static BufferMomeryInfo getBufferMomeryInfo() {
    BufferMomeryInfo bufferMomeryInfo = new BufferMomeryInfo();
    List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
    for (BufferPoolMXBean p : pools) {
      if ("direct".equalsIgnoreCase(p.getName())) {
        long count = p.getCount();
        long l = p.getMemoryUsed();
        long m = p.getTotalCapacity();
        bufferMomeryInfo.directCount = count;
        bufferMomeryInfo.directMemoryUsed = l;
        bufferMomeryInfo.totalCapacity = m;
      }
    }

    BufferMemoryStat[] requestBufferMemoryStat = bufferPool.getRequestBufferMemoryStat();
    BufferMemoryStat[] responseBufferMemoryStat = bufferPool.getResponseBufferMemoryStat();

    bufferMomeryInfo.requestBufferMemoryStat = requestBufferMemoryStat;
    bufferMomeryInfo.responseBufferMemoryStat = responseBufferMemoryStat;
    bufferMomeryInfo.responseMemoryStat = getStat();
    return bufferMomeryInfo;
  }
}