package nexus.io.tio.utils.cache;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.testng.annotations.Test;
import nexus.io.tio.utils.cache.caffeine.CaffeineCache;
import nexus.io.tio.utils.cache.caffeine.CaffeineCacheFactory;

public class CachePolicyIdentityTest {
  @Test
  public void concurrentRegistrationPublishesOneCachePerName() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(8);
    String prefix = "registration-test-" + UUID.randomUUID();
    List<Future<CaffeineCache>> futures = new ArrayList<>();
    try {
      for (int index = 0; index < 80; index++) {
        String name = prefix + (index % 10);
        futures.add(executor.submit(() -> CaffeineCacheFactory.INSTANCE.register(name, 300L, 60L)));
      }
      for (int index = 0; index < futures.size(); index++) {
        assertSame(futures.get(index).get(), CaffeineCacheFactory.INSTANCE.getCache(prefix + (index % 10)));
      }
    } finally {
      executor.shutdownNow();
      for (int index = 0; index < 10; index++) {
        CaffeineCache cache = CaffeineCacheFactory.INSTANCE.getCache(prefix + index, true);
        if (cache != null) {
          cache.clear();
        }
      }
    }
  }

  @Test
  public void differentIdlePoliciesDoNotShareAnAutomaticCache() {
    CaffeineCache first = CacheUtils.getCaffeineCache(86400L, 100L);
    CaffeineCache second = CacheUtils.getCaffeineCache(86400L, 200L);
    CaffeineCache ttlOnly = CacheUtils.getCaffeineCache(86400L, null);
    try {
      assertNotSame(first, second);
      assertNotSame(first, ttlOnly);
      first.put("same-key", "first");
      second.put("same-key", "second");
      assertEquals("first", first.get("same-key"));
      assertEquals("second", second.get("same-key"));
      assertEquals(Long.valueOf(200L), second.getTimeToIdleSeconds());
      assertSame(first, CacheUtils.getCaffeineCache(86400L, 100L));
    } finally {
      first.clear();
      second.clear();
      ttlOnly.clear();
    }
  }
}
