package nexus.io.tio.utils.token;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class HashMapTokenStorage implements ITokenStorage {

  private final Map<Object, String> storage = new ConcurrentHashMap<>();

  @Override
  public void put(Object userId, String tokenValue) {
    storage.put(TokenManager.normalizeUserId(userId), tokenValue);
  }

  @Override
  public boolean containsKey(Object userId) {
    return userId != null && storage.containsKey(TokenManager.normalizeUserId(userId));
  }

  @Override
  public String remove(Object userId) {
    return userId == null ? null : storage.remove(TokenManager.normalizeUserId(userId));
  }

  @Override
  public String get(Object userId) {
    return userId == null ? null : storage.get(TokenManager.normalizeUserId(userId));
  }

}
