package com.litongjava.tio.utils.json;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.litongjava.model.type.TioTypeReference;

public class GsonJson extends Json {

  private Gson gson = new Gson();

  @Override
  public String toJson(Object object) {
    return gson.toJson(object);
  }

  @Override
  public byte[] toJsonBytes(Object object) {
    return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public <T> T parse(String jsonString, Class<T> type) {
    return gson.fromJson(jsonString, type);
  }

  @Override
  public Map<?, ?> parseToMap(String json) {
    Type mapType = new TypeToken<Map<?, ?>>() {
    }.getType();
    return gson.fromJson(json, mapType);
  }

  @Override
  public <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    Type mapType = new TypeToken<Map<K, V>>() {
    }.getType();
    return gson.fromJson(json, mapType);
  }

  @Override
  public Object parseObject(String jsonString) {
    @SuppressWarnings("deprecation")
    JsonParser parser = new JsonParser();
    @SuppressWarnings("deprecation")
    JsonElement jsonElement = parser.parse(jsonString);
    return jsonElement.getAsJsonObject();
  }

  @SuppressWarnings("deprecation")
  @Override
  public Object parseArray(String jsonString) {
    JsonParser parser = new JsonParser();
    JsonElement jsonElement = parser.parse(jsonString);
    return jsonElement.getAsJsonArray();
  }

  @Override
  public <T> List<T> parseArray(String str, Class<T> elementType) {
    Type type = TypeToken.getParameterized(List.class, elementType).getType();
    return gson.fromJson(str, type);
  }

  @Override
  public <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType) {
    Type mapType = new TypeToken<Map<K, V>>() {
    }.getType();
    JsonArray jsonArray = JsonParser.parseString(stringValue).getAsJsonArray();
    List<Map<K, V>> listMap = new ArrayList<>();

    for (JsonElement jsonElement : jsonArray) {
      Map<K, V> map = gson.fromJson(jsonElement, mapType);
      listMap.add(map);
    }

    return listMap;
  }

  @Override
  public Object parse(String stringValue) {
    return gson.fromJson(stringValue, Object.class);
  }

  @Override
  public <T> T parse(String body, Type type) {
    return gson.fromJson(body, type);
  }

  @Override
  public <T> T parse(byte[] body, Type type) {
    String bodyString = new String(body);
    return gson.fromJson(bodyString, type);
  }

  @Override
  public <T> T parse(String body, TioTypeReference<T> tioTypeReference) {
    return gson.fromJson(body, tioTypeReference.getType());
  }
}
