package com.litongjava.tio.utils.token;

import java.util.HashMap;
import java.util.Map;

public class HashMapTokenStorage implements ITokenStorage {

  private Map<Object, String> storage = new HashMap<>();

  @Override
  public void put(Object userId, String tokenValue) {
    storage.put(userId, tokenValue);
  }

  @Override
  public boolean containsKey(Object userId) {
    return storage.containsKey(userId);
  }

  @Override
  public String remove(Object userId) {
    return storage.remove(userId);
  }

}
