package com.litongjava.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.litongjava.model.type.TioTypeReference;

/**
 * json string 与 object 互转抽象
 */
public abstract class Json {

  // private static IJsonFactory defaultJsonFactory = new JFinalJsonFactory();
  private static IJsonFactory defaultJsonFactory = new MixedJsonFactory();

  /**
   * 当对象级的 datePattern 为 null 时使用 defaultDatePattern jfinal 2.1 版本暂定
   * defaultDatePattern 值为 null，即 jackson、fastjson 默认使用自己的 date 转换策略
   */
  private static String defaultDatePattern = "yyyy-MM-dd HH:mm:ss"; // null;
  // protected String timestampPattern = "yyyy-MM-dd HH:mm:ss";
  private static String timestampPattern = null;

  /**
   * Json 继承类优先使用对象级的属性 datePattern, 然后才是全局性的 defaultDatePattern
   */
  protected String datePattern = null;
  
  //long to string
  private static boolean longToString = true;

  public static void setDefaultJsonFactory(IJsonFactory defaultJsonFactory) {
    Objects.requireNonNull(defaultJsonFactory, "defaultJsonFactory can not be null");
    Json.defaultJsonFactory = defaultJsonFactory;
  }

  public static IJsonFactory getJsonFactory() {
    return defaultJsonFactory;
  }

  public static void setDefaultDatePattern(String defaultDatePattern) {
    Json.defaultDatePattern = defaultDatePattern;
  }

  public Json setDatePattern(String datePattern) {
    this.datePattern = datePattern;
    return this;
  }

  public String getDatePattern() {
    return datePattern;
  }

  public String getDefaultDatePattern() {
    return defaultDatePattern;
  }

  public static Json getJson() {
    return defaultJsonFactory.getJson();
  }

  public static Json getSkipNullJson() {
    return defaultJsonFactory.getSkipNullJson();
  }

  public static void setTimestampPattern(String timestampPattern) {
    Json.timestampPattern = timestampPattern;
  }

  public static String getTimestampPattern() {
    return timestampPattern;
  }

  public static boolean isLongToString() {
    return longToString;
  }

  public static void setLongToString(boolean longToString) {
    Json.longToString = longToString;
  }

  public abstract String toJson(Object object);

  public abstract byte[] toJsonBytes(Object object);

  public abstract Object parse(String stringValue);

  public abstract <T> T parse(String jsonString, Class<T> type);

  public abstract Object parseObject(String jsonString);

  public abstract Object parseArray(String jsonString);

  public abstract <T> List<T> parseArray(String str, Class<T> elementType);

  public abstract Map<?, ?> parseToMap(String json);

  public abstract <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType);

  public abstract <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType);

  public abstract <T> T parse(String body, Type type);

  public abstract <T> T parse(byte[] body, Type type);

  public abstract <T> T parse(String body, TioTypeReference<T> tioTypeReference);

}
