package nexus.io.tio.utils.token;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import org.testng.annotations.*;
import nexus.io.tio.utils.jwt.JwtUtils;

public class TokenStorageRegressionTest {
  private static final String KEY = "local-regression-key";
  private ITokenStorage previous;
  @BeforeMethod public void setup() {
    previous = TokenManager.storage;
    TokenManager.setTokenStorage(new HashMapTokenStorage());
  }
  @AfterMethod public void restore() { TokenManager.setTokenStorage(previous); }

  @Test public void replacedTokensCannotAuthenticateOrExtractIdentity() {
    long expiration = System.currentTimeMillis() / 1000 + 3600;
    String first = JwtUtils.createTokenByUserId(KEY, 42L, expiration);
    String second = JwtUtils.createTokenByUserId(KEY, 42L, expiration + 1);
    TokenManager.login(42L, first);
    assertTrue(TokenManager.isLogin(KEY, first));
    TokenManager.logout(42L);
    assertFalse(TokenManager.isLogin(KEY, first));
    TokenManager.login(42L, second);
    assertFalse(TokenManager.isLogin(KEY, first));
    assertNull(TokenManager.parseUserIdInt(KEY, first));
    assertNull(TokenManager.parseUserIdLong(KEY, first));
    assertNull(TokenManager.parseUserIdString(KEY, first));
    assertTrue(TokenManager.isLogin(KEY, second));
    assertEquals(Long.valueOf(42), TokenManager.parseUserIdLong(KEY, second));
  }

  @Test public void integerAndLongIdsShareIdentityButStringIdsRemainDistinct() {
    String token = JwtUtils.createTokenByUserId(KEY, 42);
    TokenManager.login(42, token);
    assertTrue(TokenManager.isLogin(KEY, token));
    assertTrue(TokenManager.isLogin(42L));
    assertFalse(TokenManager.isLogin((Object) "42"));
    assertEquals(Integer.valueOf(42), TokenManager.parseUserIdInt(KEY, token));
    assertEquals("42", TokenManager.parseUserIdString(KEY, token));
    TokenManager.logout(42L);
    assertFalse(TokenManager.isLogin(42));
    String stringToken = JwtUtils.createTokenByUserId(KEY, "42");
    TokenManager.login("42", stringToken);
    assertTrue(TokenManager.isLogin(KEY, stringToken));
    assertFalse(TokenManager.isLogin(42));
  }

  @Test public void oldCustomStoresFailClosed() {
    TokenManager.setTokenStorage(new ITokenStorage() {
      public void put(Object id, String value) { }
      public boolean containsKey(Object id) { return true; }
      public String remove(Object id) { return null; }
    });
    assertFalse(TokenManager.isLogin(KEY, JwtUtils.createTokenByUserId(KEY, 42L)));
  }

  @Test public void customStoresReceiveCanonicalIds() {
    final Map<Object, String> values = new HashMap<>();
    TokenManager.setTokenStorage(new ITokenStorage() {
      public void put(Object id, String value) { values.put(id, value); }
      public boolean containsKey(Object id) { return values.containsKey(id); }
      public String remove(Object id) { return values.remove(id); }
      public String get(Object id) { return values.get(id); }
    });
    String token = JwtUtils.createTokenByUserId(KEY, 7);
    TokenManager.login(7, token);
    assertTrue(values.containsKey(7L));
    assertTrue(TokenManager.isLogin(KEY, token));
    TokenManager.logout((short) 7);
    assertTrue(values.isEmpty());
  }

  @Test public void parallelWritesAndRemovalsDoNotLoseOtherUsers() throws Exception {
    final HashMapTokenStorage storage = new HashMapTokenStorage();
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<?>> writes = new ArrayList<>();
      for (int t = 0; t < 8; t++) {
        final int base = t * 10000;
        writes.add(executor.submit(() -> {
          start.await();
          for (int i = 0; i < 10000; i++) storage.put(base + i, "token-" + (base + i));
          return null;
        }));
      }
      start.countDown();
      for (Future<?> write : writes) write.get(10, TimeUnit.SECONDS);
      for (int i = 0; i < 80000; i++) assertEquals("token-" + i, storage.get((long) i));
      writes.clear();
      for (int t = 0; t < 8; t++) {
        final int base = t * 10000;
        writes.add(executor.submit(() -> {
          for (int i = 0; i < 10000; i += 2) storage.remove(base + i);
        }));
      }
      for (Future<?> write : writes) write.get(10, TimeUnit.SECONDS);
      for (int i = 0; i < 80000; i++) assertEquals(i % 2 != 0, storage.containsKey(i));
    } finally { executor.shutdownNow(); }
  }
}
