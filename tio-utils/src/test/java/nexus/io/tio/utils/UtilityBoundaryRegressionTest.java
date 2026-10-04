package nexus.io.tio.utils;

import static org.junit.Assert.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.testng.annotations.Test;
import nexus.io.tio.utils.base64.Base64Utils;
import nexus.io.tio.utils.cache.CacheUtils;
import nexus.io.tio.utils.cache.caffeine.CaffeineCache;
import nexus.io.tio.utils.cache.caffeine.CaffeineCacheFactory;
import nexus.io.tio.utils.lock.LockUtils;

public class UtilityBoundaryRegressionTest {
  @Test
  public void temporaryNullPreventsRepeatedLoadsUntilInvalidated() {
    CaffeineCache cache = CaffeineCacheFactory.INSTANCE.register("negative-" + UUID.randomUUID(), 60L, null);
    AtomicInteger calls = new AtomicInteger();
    try {
      for (int i = 0; i < 3; i++) {
        String result = CacheUtils.get(cache, "missing", true, () -> { calls.incrementAndGet(); return null; });
        assertNull(result);
      }
      assertEquals(1, calls.get());
      assertNull(cache.get("missing"));
      cache.remove("missing");
      assertEquals("found", CacheUtils.get(cache, "missing", true, () -> "found"));
    } finally {
      cache.clear();
    }
  }

  @Test
  public void disabledNegativeCachingStillRetries() {
    CaffeineCache cache = CaffeineCacheFactory.INSTANCE.register("retry-" + UUID.randomUUID(), 60L, null);
    AtomicInteger calls = new AtomicInteger();
    for (int i = 0; i < 2; i++) {
      assertNull(CacheUtils.get(cache, "missing", false, () -> { calls.incrementAndGet(); return null; }));
    }
    assertEquals(2, calls.get());
  }

  @Test
  public void unicodeBase64InputIsAnArgumentError() {
    try {
      Base64Utils.decodeToBytes("AA\u4e2dA");
      fail("Expected invalid input to be rejected");
    } catch (IllegalArgumentException expected) {
      assertNotNull(expected.getMessage());
    }
    try {
      Base64Utils.altDecodeToBytes("!!\u4e2d!");
      fail("Expected invalid alternate input to be rejected");
    } catch (IllegalArgumentException expected) {
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void bothBase64AlphabetsRetainBinaryRoundTrips() {
    for (int length = 0; length < 260; length++) {
      byte[] bytes = new byte[length];
      for (int i = 0; i < length; i++) {
        bytes[i] = (byte) i;
      }
      assertEquals(java.util.Base64.getEncoder().encodeToString(bytes), Base64Utils.encodeToString(bytes));
      assertArrayEquals(bytes, Base64Utils.decodeToBytes(Base64Utils.encodeToString(bytes)));
      assertArrayEquals(bytes, Base64Utils.altDecodeToBytes(Base64Utils.byteArrayToAltBase64(bytes)));
    }
  }

  @Test
  public void interruptedLockWaitPreservesCancellation() throws Exception {
    String key = "interrupt-" + UUID.randomUUID();
    ReentrantReadWriteLock lock = LockUtils.getReentrantReadWriteLock(key, null);
    AtomicBoolean interrupted = new AtomicBoolean();
    AtomicBoolean wrote = new AtomicBoolean();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread waiter = new Thread(() -> {
      try {
        Thread.currentThread().interrupt();
        LockUtils.runWriteOrWaitRead(key, null, () -> wrote.set(true), 1L);
        interrupted.set(Thread.currentThread().isInterrupted());
      } catch (Throwable e) {
        failure.set(e);
      }
    });
    lock.writeLock().lock();
    try {
      waiter.start();
      waiter.join(3000L);
      assertFalse(waiter.isAlive());
      assertNull(failure.get());
      assertFalse(wrote.get());
      assertTrue(interrupted.get());
    } finally {
      lock.writeLock().unlock();
      waiter.interrupt();
      waiter.join(3000L);
    }
    LockUtils.runWriteOrWaitRead(key, null, () -> wrote.set(true), 1L);
    assertTrue(wrote.get());
  }
}
