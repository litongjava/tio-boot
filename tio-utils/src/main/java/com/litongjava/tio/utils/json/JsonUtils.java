package com.litongjava.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.litongjava.model.type.TioTypeReference;

public class JsonUtils {

  public static String toJson(Object object) {
    return Json.getJson().toJson(object);
  }

  public static String toSkipNullJson(Object object) {
    return Json.getSkipNullJson().toJson(object);
  }

  public static byte[] toJsonBytes(Object object) {
    return Json.getJson().toJsonBytes(object);
  }

  public static <T> T parse(String body, Class<T> type) {
    return Json.getJson().parse(body, type);
  }

  public static Object parseObject(String jsonString) {
    return Json.getJson().parseObject(jsonString);
  }

  public static Object parseArray(String jsonString) {
    return Json.getJson().parseArray(jsonString);
  }

  public static <T> List<T> parseArray(String str, Class<T> elementType) {
    return Json.getJson().parseArray(str, elementType);
  }

  public static Map<?, ?> parseToMap(String json) {
    return Json.getJson().parseToMap(json);
  }

  public static <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    return Json.getJson().parseToMap(json, kType, vType);
  }

  public static <K, V> List<Map<K, V>> parseToListMap(String json, Class<K> kType, Class<V> vType) {
    return Json.getJson().parseToListMap(json, kType, vType);
  }

  public static <T> T parse(String bodyString, Type type) {
    return Json.getJson().parse(bodyString, type);
  }

  public static <T> T parse(byte[] body, Type type) {
    return Json.getJson().parse(body, type);
  }

  public static <T> T parse(String body, TioTypeReference<T> tioTypeReference) {
    return Json.getJson().parse(body, tioTypeReference);
  }
}
