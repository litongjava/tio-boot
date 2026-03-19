package com.litongjava.tio.utils.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;

/**
 * @author Tong Li
 */
public class FastJson2Utils {

  public static String toJson(Object object) {
    return JSON.toJSONString(object);
  }

  public static byte[] toJSONBytes(Object input) {
    return JSON.toJSONBytes(input);
  }

  /**
   * 支持传入更多 SerializerFeature
   * <p>
   * 例如： SerializerFeature.WriteMapNullValue 支持对 null 值字段的转换
   */
  public static String toJson(Object input, JSONWriter.Feature... features) {
    return JSON.toJSONString(input, features);
  }

  public static byte[] toJSONBytes(Object input, JSONWriter.Feature... features) {
    return JSON.toJSONBytes(input, features);
  }

  public static <T> T parse(String jsonString, Class<T> type) {
    return JSON.parseObject(jsonString, type, JSONReader.Feature.SupportSmartMatch);
  }

  public static <T> T parse(byte[] input, Class<T> type) {
    return JSON.parseObject(input, type, JSONReader.Feature.SupportSmartMatch);
  }

  public static JSONObject parseObject(String bodyString) {
    return JSON.parseObject(bodyString);
  }

  public static JSONObject parseObject(byte[] bytes) {
    return JSON.parseObject(bytes);
  }

  public static <T> T parse(String body, TypeReference<T> tioTypeReference) {
    return JSON.parseObject(body, tioTypeReference);
  }

  public static JSONArray parseArray(String jsonString) {
    return JSON.parseArray(jsonString);
  }

  public static JSONArray parseArray(byte[] input) {
    return JSON.parseArray(input);
  }

  public static <T> List<T> parseArray(String jsonString, Class<T> clazz) {
    return JSON.parseArray(jsonString, clazz);
  }

  public static <T> List<T> parseArray(byte[] input, Class<T> clazz) {
    return JSON.parseArray(input, clazz);
  }

  public static Map<?, ?> parseToMap(String json) {
    return JSON.parseObject(json, Map.class);
  }

  public static <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    TypeReference<Map<K, V>> typeReference = new TypeReference<Map<K, V>>() {
    };

    Map<K, V> map = JSON.parseObject(json, typeReference);
    return map;
  }

  public static <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType) {
    TypeReference<Map<K, V>> typeReference = new TypeReference<Map<K, V>>() {
    };
    JSONArray jsonArray = JSON.parseArray(stringValue);
    List<Map<K, V>> listMap = new ArrayList<>();

    for (int i = 0; i < jsonArray.size(); i++) {
      Map<K, V> map = jsonArray.getJSONObject(i).to(typeReference);
      listMap.add(map);
    }

    return listMap;
  }
}
