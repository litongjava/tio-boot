package nexus.io.tio.utils.thread.pool;

import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

public class SynTaskRegressionTest {
  private static class Task extends AbstractSynRunnable {
    final AtomicInteger runs = new AtomicInteger();
    final CountDownLatch done = new CountDownLatch(1);
    CountDownLatch checked;
    Task(Executor executor) { super(executor); }
    public boolean isNeededExecute() { return false; }
    public void runTask() { runs.incrementAndGet(); done.countDown(); }
    @Override public boolean isCanceled() {
      boolean result = super.isCanceled();
      if (checked != null) checked.countDown();
      return result;
    }
  }

  private void retryRejectedTask(boolean submit) throws Exception {
    SynThreadPoolExecutor pool = new SynThreadPoolExecutor(1, 1, 1,
        new ArrayBlockingQueue<Runnable>(1), Executors.defaultThreadFactory(), "regression");
    CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    Runnable queued = () -> { };
    try {
      pool.execute(() -> {
        started.countDown();
        try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      });
      assertTrue(started.await(2, TimeUnit.SECONDS));
      pool.execute(queued);
      Task task = new Task(pool);
      try {
        if (submit) pool.submit(task, "result"); else pool.execute(task);
        fail("Expected rejection");
      } catch (RejectedExecutionException expected) { }
      assertFalse(task.executed);
      assertTrue(pool.remove(queued));
      Future<String> result = submit ? pool.submit(task, "result") : null;
      if (!submit) pool.execute(task);
      release.countDown();
      assertTrue(task.done.await(2, TimeUnit.SECONDS));
      if (submit) assertEquals("result", result.get(2, TimeUnit.SECONDS));
      assertEquals(1, task.runs.get());
    } finally { release.countDown(); pool.shutdownNow(); pool.awaitTermination(2, TimeUnit.SECONDS); }
  }

  @Test public void executeCanRetryAfterRejection() throws Exception { retryRejectedTask(false); }
  @Test public void submitCanRetryAfterRejection() throws Exception { retryRejectedTask(true); }

  @Test public void cancelWhileWaitingForLockPreventsExecution() throws Exception {
    Task task = new Task(Runnable::run);
    task.checked = new CountDownLatch(1);
    task.executed = true;
    task.runningLock.lock();
    Thread worker = new Thread(task);
    worker.start();
    try {
      assertTrue(task.checked.await(2, TimeUnit.SECONDS));
      task.setCanceled(true);
    } finally { task.runningLock.unlock(); }
    worker.join(2000);
    assertFalse(worker.isAlive());
    assertEquals(0, task.runs.get());
    assertFalse(task.executed);
  }

  @Test public void cancellationStopsRepeatLoopAndResubmission() {
    AtomicInteger submissions = new AtomicInteger();
    Task task = new Task(r -> submissions.incrementAndGet()) {
      @Override public boolean isNeededExecute() { return true; }
      @Override public void runTask() { super.runTask(); setCanceled(true); }
    };
    task.run();
    assertEquals(1, task.runs.get());
    assertEquals(0, submissions.get());
  }

  @Test public void interruptedWaitPreservesInterruptAndDoesNotResubmit() throws Exception {
    AtomicInteger submissions = new AtomicInteger();
    Task task = new Task(r -> submissions.incrementAndGet()) {
      @Override public boolean isNeededExecute() { return true; }
    };
    final boolean[] interrupted = {false};
    Thread worker = new Thread(() -> {
      Thread.currentThread().interrupt(); task.run();
      interrupted[0] = Thread.currentThread().isInterrupted();
    });
    worker.start(); worker.join(2000);
    assertTrue(interrupted[0]);
    assertEquals(0, task.runs.get());
    assertEquals(0, submissions.get());
  }
}
