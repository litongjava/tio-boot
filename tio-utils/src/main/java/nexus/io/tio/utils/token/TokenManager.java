package nexus.io.tio.utils.token;

import nexus.io.tio.utils.jwt.JwtUtils;

public class TokenManager {

  public static volatile ITokenStorage storage = new HashMapTokenStorage();
  
  public static void setTokenStorage(ITokenStorage storage) {
    TokenManager.storage=java.util.Objects.requireNonNull(storage, "storage");
  }
  public static ITokenStorage getStrorage() {
    return storage;
  }

  public static void login(Object userId, String tokenValue) {
    storage.put(java.util.Objects.requireNonNull(normalizeUserId(userId), "userId"),
        java.util.Objects.requireNonNull(tokenValue, "tokenValue"));
  }

  public static boolean isLogin(String key, String token) {
    boolean verify = JwtUtils.verify(key, token);
    if (verify) {
      Object userId = JwtUtils.parseUserId(token);
      return userId != null && token.equals(storage.get(normalizeUserId(userId)));
    }
    return false;
  }

  public static void logout(Object userId) {
    if (userId != null) storage.remove(normalizeUserId(userId));
  }

  public static boolean isLogin(Object userId) {
    return userId != null && storage.containsKey(normalizeUserId(userId));
  }

  public static Long parseUserIdLong(String key, String token) {
    if (!isLogin(key, token)) return null;
    try { return JwtUtils.parseUserIdLong(token); }
    catch (NumberFormatException e) { return null; }
  }

  public static String parseUserIdString(String key, String token) {
    return isLogin(key, token) ? JwtUtils.parseUserIdString(token) : null;
  }

  public static Integer parseUserIdInt(String key, String token) {
    if (!isLogin(key, token)) return null;
    try { return JwtUtils.parseUserIdInt(token); }
    catch (NumberFormatException e) { return null; }
  }

  /** Integral JVM IDs share an identity; string IDs remain distinct. */
  static Object normalizeUserId(Object userId) {
    if (userId instanceof Byte || userId instanceof Short || userId instanceof Integer) {
      return ((Number) userId).longValue();
    }
    return userId;
  }
}
