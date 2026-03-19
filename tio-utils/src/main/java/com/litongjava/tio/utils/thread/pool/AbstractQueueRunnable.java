package com.litongjava.tio.utils.thread.pool;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.queue.FullWaitQueue;

/**
 *
 * @author tanyaowu
 * 2017年4月4日 上午9:23:12
 */
public abstract class AbstractQueueRunnable<T> extends AbstractSynRunnable {
  private static final Logger log = LoggerFactory.getLogger(AbstractQueueRunnable.class);

  public AbstractQueueRunnable(Executor executor) {
    super(executor);
  }

  /**
   * @return
   *
   */
  public boolean addMsg(T t) {
    if (this.isCanceled()) {
      log.error("任务已经取消");
      return false;
    }

    return getMsgQueue().add(t);
  }

  /**
   * 清空处理的队列消息
   */
  public void clearMsgQueue() {
    if (getMsgQueue() != null) {
      getMsgQueue().clear();
    }
  }

  @Override
  public boolean isNeededExecute() {
    return (getMsgQueue() != null && !getMsgQueue().isEmpty()) && !this.isCanceled();
  }

  /**
   * 获取消息队列
   * @return
   */
  public abstract FullWaitQueue<T> getMsgQueue();
}
