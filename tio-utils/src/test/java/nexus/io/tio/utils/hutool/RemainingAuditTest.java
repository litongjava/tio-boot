package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.testng.annotations.Test;
import nexus.io.tio.utils.cache.CacheUtils;
import nexus.io.tio.utils.cache.caffeine.CaffeineCache;
import nexus.io.tio.utils.cache.caffeine.CaffeineCacheFactory;
import nexus.io.tio.utils.lock.LockUtils;

public class RemainingAuditTest {
  @Test public void directoryLinksDoNotExposeReferentContentsToCleanup() throws Exception {
    Path parent = Paths.get("target").toAbsolutePath().toRealPath();
    Path root = Files.createTempDirectory(parent, "cleanup-test-");
    try {
      Path outside = Files.createDirectory(root.resolve("outside"));
      Path marker = Files.write(outside.resolve("keep"), new byte[] {1});
      Path folder = Files.createDirectory(root.resolve("folder"));
      Path nested = Files.createDirectory(folder.resolve("nested"));
      Files.write(nested.resolve("ordinary"), new byte[] {2});
      Path link = folder.resolve("link");
      Files.createSymbolicLink(link, outside);
      assertTrue(FileUtil.clean(folder.toFile()));
      assertTrue(Files.exists(marker));
      assertTrue(Files.isDirectory(folder));
      assertFalse(Files.exists(nested));
      assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
      Files.createSymbolicLink(link, outside);
      assertTrue(FileUtil.del(link.toFile()));
      assertTrue(Files.exists(marker));
      Files.createSymbolicLink(link, outside);
      assertTrue(FileUtil.clean(link.toFile()));
      assertTrue(Files.exists(marker));
      assertTrue(Files.isSymbolicLink(link));
      Files.delete(link);
      Files.createSymbolicLink(link, root.resolve("missing"));
      assertTrue(FileUtil.del(link.toFile()));
      assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
    } finally {
      if (!root.toRealPath().startsWith(parent) || root.equals(parent)) {
        throw new IOException("Invalid fixture path");
      }
      Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
        @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
          Files.delete(path); return FileVisitResult.CONTINUE;
        }
        @Override public FileVisitResult postVisitDirectory(Path path, IOException error) throws IOException {
          if (error != null) { throw error; }
          Files.delete(path); return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  @Test public void independentCacheNamesAndKeysDoNotShareLoads() throws Exception {
    String prefix = "collision-" + UUID.randomUUID();
    CaffeineCache a = CaffeineCacheFactory.INSTANCE.register(prefix + "a", 60L, null);
    CaffeineCache ab = CaffeineCacheFactory.INSTANCE.register(prefix + "ab", 60L, null);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<String> first = executor.submit(() -> CacheUtils.get(a, "bc", false, () -> {
      entered.countDown();
      if (!release.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Timeout"); }
      return "first";
    }, 0L));
    try {
      assertTrue(entered.await(3, TimeUnit.SECONDS));
      assertEquals("second", CacheUtils.get(ab, "c", false, () -> "second", 0L));
    } finally {
      release.countDown(); first.get(5, TimeUnit.SECONDS); executor.shutdownNow(); a.clear(); ab.clear();
    }
  }

  @Test public void loaderFailureRemainsObservableAndCanRetry() {
    CaffeineCache cache = CaffeineCacheFactory.INSTANCE.register("failure-" + UUID.randomUUID(), 60L, null);
    IOException failure = new IOException("load failed");
    try {
      CacheUtils.get(cache, "key", true, () -> { throw failure; });
      fail("Loader failure must propagate");
    } catch (RuntimeException expected) {
      assertSame(failure, expected.getCause());
    }
    assertEquals("retry", CacheUtils.get(cache, "key", true, () -> "retry"));
    cache.clear();
  }

  @Test public void concurrentLookupsShareLocksWithDifferentCallerMonitors() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(16);
    try {
      for (int round = 0; round < 30; round++) {
        String key = "race-" + UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object[]>> results = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
          results.add(executor.submit(() -> {
            start.await();
            return new Object[] {LockUtils.getLockObj(key, new Object()), LockUtils.getReentrantReadWriteLock(key, new Object())};
          }));
        }
        start.countDown();
        Object[] first = results.get(0).get();
        for (Future<Object[]> result : results) {
          Object[] locks = result.get(); assertSame(first[0], locks[0]); assertSame(first[1], locks[1]);
        }
      }
    } finally { executor.shutdownNow(); }
  }

  @Test public void retainedWriteHandleKeepsOwnerLockReachable() {
    String key = "handle-" + UUID.randomUUID();
    ReentrantReadWriteLock lock = LockUtils.getReentrantReadWriteLock(key, null);
    ReentrantReadWriteLock.WriteLock handle = lock.writeLock();
    java.lang.ref.WeakReference<ReentrantReadWriteLock> owner = new java.lang.ref.WeakReference<>(lock);
    lock = null;
    handle.lock();
    try {
      System.gc();
      assertNotNull(owner.get());
      assertSame(owner.get(), LockUtils.getReentrantReadWriteLock(key, new Object()));
    } finally { handle.unlock(); }
  }

  @Test public void gbkSlicingFollowsActualCharacterBoundaries() {
    assertEquals("\u4e02\u4e2d!", StrUtil.subPreGbk("\u4e02\u4e2dX", 3, "!"));
    assertEquals("\u4e02!", StrUtil.subPreGbk("\u4e02X", 2, "!"));
    assertEquals("\u4e2d!", StrUtil.subPreGbk("\u4e2dX", 1, "!"));
    assertEquals("!", StrUtil.subPreGbk("abc", 0, "!"));
    assertEquals("abc", StrUtil.subPreGbk("abc", 10, "!"));
  }

  @Test public void extremeSubstringBoundsDoNotWrapAround() {
    assertEquals("", StrUtil.subWithLength("abcdef", Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertEquals("bcdef", StrUtil.subWithLength("abcdef", 1, Integer.MAX_VALUE));
    assertEquals("bc", StrUtil.subWithLength("abcdef", 1, 2));
    assertEquals("cd", StrUtil.subWithLength("abcdef", -4, 2));
    assertNull(StrUtil.subWithLength(null, 1, 2));
  }
  @Test public void retainedReadHandleKeepsOwnerLockReachable() {
    String key = "read-handle-" + UUID.randomUUID();
    ReentrantReadWriteLock lock = LockUtils.getReentrantReadWriteLock(key, null);
    ReentrantReadWriteLock.ReadLock handle = lock.readLock();
    java.lang.ref.WeakReference<ReentrantReadWriteLock> owner = new java.lang.ref.WeakReference<>(lock);
    lock = null;
    handle.lock();
    try {
      System.gc();
      assertNotNull(owner.get());
      assertSame(owner.get(), LockUtils.getReentrantReadWriteLock(key, new Object()));
    } finally { handle.unlock(); }
  }

  @Test public void runtimeLoaderFailureIsNotWrappedOrCached() {
    CaffeineCache cache = CaffeineCacheFactory.INSTANCE.register("runtime-" + UUID.randomUUID(), 60L, null);
    IllegalArgumentException failure = new IllegalArgumentException("invalid source");
    try {
      CacheUtils.get(cache, "key", true, () -> { throw failure; });
      fail("Expected failure");
    } catch (RuntimeException expected) { assertSame(failure, expected); }
    assertEquals("retry", CacheUtils.get(cache, "key", true, () -> "retry"));
    cache.clear();
  }
}
