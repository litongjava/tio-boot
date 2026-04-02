package com.litongjava.tio.utils.json;

/**
 * IJsonFactory 的 jackson 实现.
 */
public class JacksonFactory implements IJsonFactory {

  private static final JacksonFactory me = new JacksonFactory();

  public static JacksonFactory me() {
    return me;
  }

  public Json getJson() {
    return new Jackson(true);
  }

  @Override
  public Json getSkipNullJson() {
    return new Jackson(false);
  }
}
