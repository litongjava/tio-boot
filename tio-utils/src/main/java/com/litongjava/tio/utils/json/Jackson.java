package com.litongjava.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.litongjava.model.type.TioTypeReference;
import com.litongjava.tio.utils.date.TioTimeUtils;

/**
 * Json 转换 jackson 实现.
 * <p>
 * json 到 java 类型转换规则: http://wiki.fasterxml.com/JacksonInFiveMinutes JSON TYPE
 * JAVA TYPE object LinkedHashMap<String,Object> array ArrayList<Object> string
 * String number (no fraction) Integer, Long or BigInteger (smallest applicable)
 * number (fraction) Double (configurable to use BigDecimal) true|false Boolean
 * null null
 */
@SuppressWarnings("deprecation")
public class Jackson extends Json {

  // Jackson 生成 json 的默认行为是生成 null value，可设置此值全局改变默认行为
  private static boolean defaultGenerateNullValue = true;

  // generateNullValue 通过设置此值，可临时改变默认生成 null value 的行为
  protected Boolean generateNullValue = null;

  protected static final ObjectMapper objectMapper = new ObjectMapper();

  // https://gitee.com/jfinal/jfinal-weixin/issues/I875U
  static {
    objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

    // 没有 getter 方法时不抛异常
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  public static void setDefaultGenerateNullValue(boolean defaultGenerateNullValue) {
    Jackson.defaultGenerateNullValue = defaultGenerateNullValue;
  }

  public Jackson setGenerateNullValue(boolean generateNullValue) {
    this.generateNullValue = generateNullValue;
    return this;
  }

  /**
   * 通过获取 ObjectMapper 进行更个性化设置，满足少数特殊情况
   */
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static Jackson getJson() {
    return new Jackson();
  }

  public String toJson(Object object) {
    try {
      // 优先使用对象级的属性 datePattern, 然后才是全局性的 defaultDatePattern
      String dp = datePattern != null ? datePattern : getDefaultDatePattern();
      if (dp != null) {
        objectMapper.setDateFormat(TioTimeUtils.getSimpleDateFormat(dp));
      }

      // 优先使用对象属性 generateNullValue，决定转换 json时是否生成 null value
      boolean pnv = generateNullValue != null ? generateNullValue : defaultGenerateNullValue;
      if (!pnv) {
        objectMapper.setSerializationInclusion(Include.NON_NULL);
      }

      return objectMapper.writeValueAsString(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
  public byte[] toJsonBytes(Object object) {
    try {
      // 优先使用对象级的属性 datePattern, 然后才是全局性的 defaultDatePattern
      String dp = datePattern != null ? datePattern : getDefaultDatePattern();
      if (dp != null) {
        objectMapper.setDateFormat(TioTimeUtils.getSimpleDateFormat(dp));
      }

      // 优先使用对象属性 generateNullValue，决定转换 json时是否生成 null value
      boolean pnv = generateNullValue != null ? generateNullValue : defaultGenerateNullValue;
      if (!pnv) {
        objectMapper.setSerializationInclusion(Include.NON_NULL);
      }

      return objectMapper.writeValueAsBytes(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public <T> T parse(String jsonString, Class<T> type) {
    try {
      return objectMapper.readValue(jsonString, type);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
  public Map<?, ?> parseToMap(String json) {
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
  public <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    TypeReference<Map<K, V>> typeReference = new TypeReference<Map<K, V>>() {
    };
    try {
      return objectMapper.readValue(json, typeReference);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
  public Object parseObject(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Object parseArray(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public <T> List<T> parseArray(String str, Class<T> elementType) {
    try {
      return objectMapper.readValue(str, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType) {
    try {
      TypeReference<List<Map<K, V>>> typeReference = new TypeReference<List<Map<K, V>>>() {
      };
      return objectMapper.readValue(stringValue, typeReference);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
  public Object parse(String stringValue) {
    try {
      return objectMapper.readValue(stringValue, Object.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T parse(String body, Type type) {
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type);
      return objectMapper.readValue(body, javaType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T parse(byte[] body, Type type) {
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    try {
      return objectMapper.readValue(body, javaType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T parse(String body, TioTypeReference<T> tioTypeReference) {
    Type type = tioTypeReference.getType();
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    try {
      return objectMapper.readValue(body, javaType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
