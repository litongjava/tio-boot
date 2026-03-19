package com.litongjava.tio.utils.json;

/**
 * IJsonFactory 的 jfinal 实现.
 */
public class TioJsonFactory implements IJsonFactory {

  private static final TioJsonFactory me = new TioJsonFactory();

  public static TioJsonFactory me() {
    return me;
  }

  public Json getJson() {
    return new TioJson();
  }

  @Override
  public Json getSkipNullJson() {
    return new TioJson(false);
  }
}
