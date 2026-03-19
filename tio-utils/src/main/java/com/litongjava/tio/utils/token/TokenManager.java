package com.litongjava.tio.utils.token;

import com.litongjava.tio.utils.jwt.JwtUtils;

public class TokenManager {

  public static ITokenStorage storage = new HashMapTokenStorage();
  
  public static void setTokenStorage(ITokenStorage storage) {
    TokenManager.storage=storage;
  }
  public static ITokenStorage getStrorage() {
    return storage;
  }

  public static void login(Object userId, String tokenValue) {
    storage.put(userId, tokenValue);
  }

  public static boolean isLogin(String key, String token) {
    boolean verify = JwtUtils.verify(key, token);
    if (verify) {
      Object userId = JwtUtils.parseUserId(token);
      return storage.containsKey(userId);
    }
    return false;
  }

  public static void logout(Object userId) {
    storage.remove(userId);
  }

  public static boolean isLogin(Object userId) {
    return storage.containsKey(userId);
  }

  public static Long parseUserIdLong(String key, String token) {
    boolean verify = JwtUtils.verify(key, token);
    if (verify) {
      Long userId = JwtUtils.parseUserIdLong(token);
      if (storage.containsKey(userId)) {
        return userId;
      }
    }
    return null;
  }

  public static String parseUserIdString(String key, String token) {
    boolean verify = JwtUtils.verify(key, token);
    if (verify) {
      String userId = JwtUtils.parseUserIdString(token);
      if (storage.containsKey(userId)) {
        return userId;
      }
    }
    return null;
  }

  public static Integer parseUserIdInt(String key, String token) {
    boolean verify = JwtUtils.verify(key, token);
    if (verify) {
      Integer userId = JwtUtils.parseUserIdInt(token);
      if (storage.containsKey(userId)) {
        return userId;
      }
    }
    return null;
  }
}
