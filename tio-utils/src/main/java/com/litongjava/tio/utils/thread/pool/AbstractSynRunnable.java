package com.litongjava.tio.utils.thread.pool;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tanyaowu
 */
public abstract class AbstractSynRunnable implements Runnable {
  /** The log. */
  private static Logger log = LoggerFactory.getLogger(AbstractSynRunnable.class);
  /**
   * 是否已经提交到线程池了
   */
  public boolean executed = false;
  protected ReentrantLock runningLock = new ReentrantLock();
  public Executor executor;
  private boolean isCanceled = false;

  /**
   * Instantiates a new abstract syn runnable.
   */
  protected AbstractSynRunnable(Executor executor) {
    this.executor = executor;
  }

  /**
   * 把本任务对象提交到线程池去执行
   * @author tanyaowu
   */
  public void execute() {
    executor.execute(this);
  }

  public abstract boolean isNeededExecute();

  public boolean isCanceled() {
    return isCanceled;
  }

  @Override
  public final void run() {
    if (isCanceled()) //任务已经被取消
    {
      return;
    }
    boolean tryLock = false;
    try {
      tryLock = runningLock.tryLock(1L, TimeUnit.SECONDS);
    } catch (InterruptedException e1) {
      log.error(e1.toString(), e1);
    }
    if (tryLock) {
      try {
        int loopCount = 0;
        runTask();
        while (isNeededExecute() && loopCount++ < 100) {
          runTask();
        }

      } catch (Throwable e) {
        log.error(e.toString(), e);
      } finally {
        executed = false;
        runningLock.unlock();
      }
    } else {
      executed = false;
    }

    //下面这段代码一定要在unlock()后面，别弄错了 ^_^
    if (isNeededExecute()) {
      execute();
    }

  }

  /**
   * 
   * @author tanyaowu
   */
  public abstract void runTask();

  public void setCanceled(boolean isCanceled) {
    this.isCanceled = isCanceled;
  }
  //
  //	/**
  //	 * 是否已经提交到线程池了
  //	 * @return the executed
  //	 */
  //	public boolean isExecuted() {
  //		return executed;
  //	}
  //
  //	/**
  //	 * 是否已经提交到线程池了
  //	 * @param executed the executed to set
  //	 */
  //	public void setExecuted(boolean executed) {
  //		this.executed = executed;
  //	}

  public String logstr() {
    return this.getClass().getName();
  }
}
