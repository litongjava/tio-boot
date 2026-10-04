package nexus.io.tio.utils;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.testng.annotations.Test;
import nexus.io.tio.utils.lock.*;
import nexus.io.tio.utils.queue.TioFullWaitQueue;

public class ConcurrentContainerRegressionTest {
  @Test public void suppliedLockCoordinatesHandlersAcrossMaps() throws Exception {
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    MapWithLock<String, String> first = new MapWithLock<>(Collections.singletonMap("old", "value"), lock);
    MapWithLock<String, String> second = new MapWithLock<>(new HashMap<>(), lock);
    assertSame(lock, first.getLock()); assertSame(lock, second.getLock());
    assertEquals("value", first.get("old"));
    CountDownLatch started = new CountDownLatch(1), entered = new CountDownLatch(1);
    first.writeLock().lock();
    Thread worker = new Thread(() -> {
      started.countDown();
      second.handle((WriteLockHandler<Map<String, String>>) m -> { m.put("new", "value"); entered.countDown(); });
    });
    worker.start();
    try {
      assertTrue(started.await(2, TimeUnit.SECONDS));
      assertFalse(entered.await(150, TimeUnit.MILLISECONDS));
    } finally { first.writeLock().unlock(); }
    worker.join(2000);
    assertFalse(worker.isAlive());
    assertEquals(0, entered.getCount());
    assertEquals("value", second.get("new"));
  }

  @Test public void boundedQueueWaitsUntilConsumerFreesSpace() throws Exception {
    TioFullWaitQueue<Integer> queue = new TioFullWaitQueue<>(1, false);
    assertTrue(queue.add(1));
    ExecutorService producer = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> added = producer.submit(() -> queue.add(2));
      try { added.get(100, TimeUnit.MILLISECONDS); fail("Full queue accepted an item"); }
      catch (TimeoutException expected) { }
      assertEquals(1, queue.size());
      assertEquals(Integer.valueOf(1), queue.poll());
      assertTrue(added.get(2, TimeUnit.SECONDS));
      assertEquals(Integer.valueOf(2), queue.poll());
      assertTrue(queue.isEmpty());
    } finally { producer.shutdownNow(); }
  }

  @Test public void fullQueueTimesOutAndClearAllowsReuse() {
    TioFullWaitQueue<Integer> queue = new TioFullWaitQueue<>(1, true);
    assertTrue(queue.add(1));
    assertFalse(queue.add(2));
    assertEquals(1, queue.size());
    queue.clear();
    assertTrue(queue.add(3));
    assertEquals(Integer.valueOf(3), queue.poll());
    for (int capacity : new int[] {0, -1}) {
      try { new TioFullWaitQueue<Integer>(capacity, false); fail("Invalid capacity accepted"); }
      catch (IllegalArgumentException expected) { }
    }
    TioFullWaitQueue<Integer> unbounded = new TioFullWaitQueue<>(null, false);
    for (int i = 0; i < 100; i++) assertTrue(unbounded.add(i));
    assertEquals(100, unbounded.size());
  }

  @Test public void interruptedProducerDoesNotEnqueueAndKeepsInterrupt() throws Exception {
    TioFullWaitQueue<Integer> queue = new TioFullWaitQueue<>(1, false);
    queue.add(1);
    final boolean[] results = new boolean[2];
    Thread producer = new Thread(() -> {
      Thread.currentThread().interrupt();
      results[0] = queue.add(2);
      results[1] = Thread.currentThread().isInterrupted();
    });
    producer.start(); producer.join(2000);
    assertFalse(producer.isAlive());
    assertFalse(results[0]); assertTrue(results[1]);
    assertEquals(1, queue.size());
  }
}
