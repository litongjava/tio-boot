package com.litongjava.tio.utils.json;

public class GsonFactory implements IJsonFactory {

  private static final GsonFactory me = new GsonFactory();

  public static GsonFactory me() {
    return me;
  }

  public GsonFactory() {
  }

  @Override
  public Json getJson() {
    return new GsonJson();
  }

  @Override
  public Json getSkipNullJson() {
    return null;
  }
}
