package com.litongjava.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.litongjava.model.type.TioTypeReference;

/**
 * JFinalJson 与 FastJson 混合做 json 转换 toJson 用 JFinalJson，parse 用 FastJson
 * <p>
 * 注意： 1：需要添加 fastjson 相关 jar 包 2：parse 方法转对象依赖于 setter 方法
 */
public class MixedJson extends Json {

  private TioJson tioJson;
  private TioJson SkipNullTioJson;
  private FastJson2 fastJson;
  private boolean skipNullValueField;

  public MixedJson() {

  }

  public MixedJson(boolean skipNullValueField) {
    this.skipNullValueField = skipNullValueField;
  }

  public static MixedJson getJson() {
    return new MixedJson();
  }

  public String toJson(Object object) {
    if(skipNullValueField) {
      return getSkipNullTioJson().toJson(object);
    }else {
      return getTioJson().toJson(object);
    }
  }

  public <T> T parse(String jsonString, Class<T> type) {
    return getFastJson().parse(jsonString, type);
  }

  private TioJson getTioJson() {
    if (tioJson == null) {
      tioJson = TioJson.getJson();
    }
    if (datePattern != null) {
      tioJson.setDatePattern(datePattern);
    }
    return tioJson;
  }

  public TioJson getSkipNullTioJson() {
    if (SkipNullTioJson == null) {
      SkipNullTioJson = TioJson.getSkipNullTioJson();
    }
    if (datePattern != null) {
      SkipNullTioJson.setDatePattern(datePattern);
    }
    return SkipNullTioJson;
  }

  private FastJson2 getFastJson() {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    if (datePattern != null) {
      fastJson.setDatePattern(datePattern);
    }
    return fastJson;
  }

  @Override
  public Map<?, ?> parseToMap(String json) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parseToMap(json);
  }

  @Override
  public <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parseToMap(json, kType, vType);
  }

  @Override
  public Object parseObject(String jsonString) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parseObject(jsonString);
  }

  @Override
  public Object parseArray(String jsonString) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parseArray(jsonString);
  }

  @Override
  public <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parseToListMap(stringValue, kType, vType);
  }

  @Override
  public Object parse(String stringValue) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parse(stringValue);
  }

  @Override
  public byte[] toJsonBytes(Object object) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.toJsonBytes(object);
  }

  @Override
  public <T> T parse(String body, Type type) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parse(body, type);
  }

  @Override
  public <T> T parse(byte[] body, Type type) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parse(body, type);
  }

  @Override
  public <T> List<T> parseArray(String str, Class<T> elementType) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parseArray(str, elementType);
  }

  @Override
  public <T> T parse(String body, TioTypeReference<T> tioTypeReference) {
    if (fastJson == null) {
      fastJson = FastJson2.getJson();
    }
    return fastJson.parse(body, tioTypeReference);
  }
}
