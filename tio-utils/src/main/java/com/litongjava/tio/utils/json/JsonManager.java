package com.litongjava.tio.utils.json;

/**
 * JsonManager.
 */
public class JsonManager {

  private static final JsonManager me = new JsonManager();

  private JsonManager() {
  }

  public static JsonManager me() {
    return me;
  }

  public void setDefaultJsonFactory(IJsonFactory defaultJsonFactory) {
    Json.setDefaultJsonFactory(defaultJsonFactory);
  }

  public void setDefaultDatePattern(String defaultDatePattern) {
    Json.setDefaultDatePattern(defaultDatePattern);
  }

}
