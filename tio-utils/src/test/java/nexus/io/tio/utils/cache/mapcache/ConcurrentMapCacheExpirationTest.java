package nexus.io.tio.utils.cache.mapcache;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.Test;
import nexus.io.tio.utils.cache.RemovalCause;

public class ConcurrentMapCacheExpirationTest {
  @Test
  public void idleRefreshNeverExtendsTheTtlAndExpiredReadsCannotRevive() {
    AtomicLong now = new AtomicLong(1000000);
    AtomicInteger removals = new AtomicInteger();
    ConcurrentMapCache cache = new ConcurrentMapCache("ttl", 100L, 80L,
        (key, value, cause) -> {
          assertEquals(RemovalCause.EXPIRED, cause);
          removals.incrementAndGet();
        }, now::get);
    try {
      cache.put("key", "value");
      now.addAndGet(70000);
      assertEquals("value", cache.get("key"));
      assertEquals(30000, cache.ttl("key"));
      now.addAndGet(30000);
      assertNull(cache.get("key"));
      assertNull(cache.get("key"));
      assertEquals(1, removals.get());
      assertEquals(0, cache.size());
    } finally {
      cache.clear();
    }
  }

  @Test
  public void inspectionDoesNotRefreshIdleTime() {
    AtomicLong now = new AtomicLong(1000000);
    ConcurrentMapCache cache = new ConcurrentMapCache("idle", null, 100L, null, now::get);
    try {
      cache.put("key", "value");
      now.addAndGet(90000);
      assertEquals(10000, cache.ttl("key"));
      assertEquals(1, cache.size());
      assertTrue(cache.keysCollection().contains("key"));
      now.addAndGet(10000);
      assertNull(cache.get("key"));
      assertEquals(-1, cache.ttl("key"));
    } finally {
      cache.clear();
    }
  }

  @Test
  public void refreshedIdleEntryIsEventuallyRemovedWithoutAnotherRead() throws Exception {
    CountDownLatch expired = new CountDownLatch(1);
    ConcurrentMapCache cache = new ConcurrentMapCache("reschedule", null, 1L,
        (key, value, cause) -> expired.countDown());
    try {
      cache.put("key", "value");
      Thread.sleep(400);
      assertEquals("value", cache.get("key"));
      assertFalse(expired.await(650, TimeUnit.MILLISECONDS));
      assertTrue("The postponed expiration task must run", expired.await(3, TimeUnit.SECONDS));
    } finally {
      cache.clear();
    }
  }

  @Test
  public void oldExpirationTaskCannotRemoveReplacement() throws Exception {
    AtomicLong now = new AtomicLong(1000000);
    ConcurrentMapCache cache = new ConcurrentMapCache("replacement", 100L, null, null, now::get);
    try {
      cache.put("key", "old");
      Field field = ConcurrentMapCache.class.getDeclaredField("expirations");
      field.setAccessible(true);
      Map<?, ?> expirations = (Map<?, ?>) field.get(cache);
      Object old = expirations.get("key");
      now.addAndGet(50000);
      cache.put("key", "new");
      now.addAndGet(50000);
      Method expire = ConcurrentMapCache.class.getDeclaredMethod("expire", String.class, old.getClass());
      expire.setAccessible(true);
      expire.invoke(cache, "key", old);
      assertEquals("new", cache.get("key"));
      assertEquals(50000, cache.ttl("key"));
    } finally {
      cache.clear();
    }
  }

  @Test
  public void unboundedAndVeryLongLifetimesDoNotOverflow() {
    AtomicLong now = new AtomicLong(1000000);
    ConcurrentMapCache cache = new ConcurrentMapCache("unbounded", null, null, null, now::get);
    try {
      cache.put("key", "value");
      assertEquals(-1, cache.ttl("key"));
      now.addAndGet(1000000);
      assertEquals("value", cache.get("key"));
      cache.setTimeToLiveSeconds(Long.MAX_VALUE);
      cache.put("huge", "value");
      assertEquals("value", cache.get("huge"));
      cache.clear();
      assertTrue(cache.asMap().isEmpty());
    } finally {
      cache.clear();
    }
  }

  @Test
  public void explicitRemovalNotifiesOnceOutsideCacheLock() {
    AtomicReference<ConcurrentMapCache> reference = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();
    ConcurrentMapCache cache = new ConcurrentMapCache("remove", 100L, null, (key, value, cause) -> {
      assertFalse(Thread.holdsLock(reference.get()));
      assertEquals(RemovalCause.EXPLICIT, cause);
      calls.incrementAndGet();
    });
    reference.set(cache);
    try {
      cache.put("key", "value");
      cache.remove("key");
      cache.remove("key");
      assertEquals(1, calls.get());
      assertEquals(-1, cache.ttl("key"));
    } finally {
      cache.clear();
    }
  }
}
