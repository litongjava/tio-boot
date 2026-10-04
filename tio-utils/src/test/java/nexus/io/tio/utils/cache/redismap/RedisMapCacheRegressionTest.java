package nexus.io.tio.utils.cache.redismap;

import static org.junit.Assert.*;
import java.util.*;
import org.testng.SkipException;
import org.testng.annotations.*;
import redis.clients.jedis.JedisPool;

/** Run against an isolated Redis: -Daudit.redis.port=PORT. Never flushes the database. */
public class RedisMapCacheRegressionTest {
  private JedisPool previous;
  private JedisPool pool;
  private final List<RedisMapCache> caches = new ArrayList<>();
  private final String prefix = "audit-" + UUID.randomUUID();

  @BeforeClass public void connect() {
    String port = System.getProperty("audit.redis.port");
    if (port == null) throw new SkipException("Set audit.redis.port to an isolated Redis port");
    previous = JedisPoolCan.jedisPool;
    pool = new JedisPool("127.0.0.1", Integer.parseInt(port));
    JedisPoolCan.jedisPool = pool;
    try (redis.clients.jedis.Jedis jedis = pool.getResource()) { assertEquals("PONG", jedis.ping()); }
  }

  private RedisMapCache cache(String suffix, Long ttl, Long idle) {
    RedisMapCache cache = new RedisMapCache(prefix + suffix, ttl, idle, null);
    caches.add(cache);
    return cache;
  }

  @AfterClass(alwaysRun = true) public void close() {
    if (pool == null) return;
    try { for (RedisMapCache cache : caches) cache.clear(); }
    finally { JedisPoolCan.jedisPool = previous; pool.close(); }
  }

  @Test public void namespacesAndAllEnumerationMethodsAreIsolated() {
    RedisMapCache parent = cache("foo", null, null);
    RedisMapCache nested = cache("foo:bar", null, null);
    RedisMapCache glob = cache("*?[x]\\", null, null);
    RedisMapCache unicode = cache("中文", null, null);
    parent.put("bar:key", "parent");
    nested.put("key", "nested");
    glob.put("key", "glob");
    unicode.put("中文键", "value");
    assertEquals(1, parent.size());
    assertEquals(Collections.singleton("bar:key"), new HashSet<>(parent.keysCollection()));
    assertEquals(Collections.singletonMap("key", "nested"), nested.asMap());
    assertEquals("bar:key", parent.keys().iterator().next());
    parent.clear();
    assertEquals("nested", nested.get("key"));
    glob.clear();
    assertEquals("nested", nested.get("key"));
    assertEquals("value", unicode.get("中文键"));
    unicode.remove("中文键");
    assertNull(unicode.get("中文键"));
  }

  @Test public void frequentReadsCannotExtendFixedDeadline() throws Exception {
    RedisMapCache cache = cache("deadline", 3L, 2L);
    cache.put("key", "value");
    long start = System.nanoTime();
    while (System.nanoTime() - start < 2_200_000_000L) {
      assertEquals("value", cache.get("key"));
      Thread.sleep(200);
    }
    while (System.nanoTime() - start < 3_300_000_000L) {
      cache.get("key");
      Thread.sleep(100);
    }
    assertNull(cache.get("key"));
  }

  @Test public void idleOnlyRefreshesAndPerEntryTtlWins() throws Exception {
    RedisMapCache idle = cache("idle", null, 1L);
    idle.put("key", "value");
    for (int i = 0; i < 3; i++) {
      Thread.sleep(400);
      assertEquals("value", idle.get("key"));
    }
    Thread.sleep(1100);
    assertNull(idle.get("key"));
    RedisMapCache override = cache("override", 60L, 10L);
    override.put("key", "value", 1);
    Thread.sleep(500);
    assertEquals("value", override.get("key"));
    Thread.sleep(650);
    assertNull(override.get("key"));
  }

  @Test public void replacementResetsDeadlineAndPersistentEntriesRemainPersistent() throws Exception {
    RedisMapCache cache = cache("replace", null, null);
    cache.put("key", "old", 1);
    cache.put("key", "new");
    Thread.sleep(1100);
    assertEquals("new", cache.get("key"));
    assertEquals(Collections.singletonMap("key", "new"), cache.asMap());
    cache.remove("key");
    assertNull(cache.get("key"));
  }
}
