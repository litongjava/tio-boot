package com.litongjava.tio.utils.token;

/**
 * ITokenCache.
 */
public interface ITokenStorage {

  void put(Object userId, String tokenValue);

  boolean containsKey(Object userId);

  String remove(Object userId);
}