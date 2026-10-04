package nexus.io.tio.utils.token;

/**
 * ITokenCache.
 */
public interface ITokenStorage {

  void put(Object userId, String tokenValue);

  boolean containsKey(Object userId);

  /**
   * Returns the current token. Custom stores must implement this to support
   * token authentication. The default fails closed for legacy implementations.
   */
  default String get(Object userId) { return null; }

  String remove(Object userId);
}
