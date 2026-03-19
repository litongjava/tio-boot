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
public class JacksonUtils {

  // Jackson 生成 json 的默认行为是生成 null value，可设置此值全局改变默认行为
  private static boolean defaultGenerateNullValue = true;

  // generateNullValue 通过设置此值，可临时改变默认生成 null value 的行为
  protected static Boolean generateNullValue = null;

  protected static final ObjectMapper objectMapper = new ObjectMapper();

  // https://gitee.com/jfinal/jfinal-weixin/issues/I875U
  static {
    objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

    // 没有 getter 方法时不抛异常
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  public static void setDefaultGenerateNullValue(boolean defaultGenerateNullValue) {
    JacksonUtils.defaultGenerateNullValue = defaultGenerateNullValue;
  }

  public static void setGenerateNullValue(boolean generateNullValue) {
    JacksonUtils.generateNullValue = generateNullValue;
  }

  /**
   * 通过获取 ObjectMapper 进行更个性化设置，满足少数特殊情况
   */
  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static JacksonUtils getJson() {
    return new JacksonUtils();
  }

  public static String toJson(Object object) {
    // 优先使用对象属性 generateNullValue，决定转换 json时是否生成 null value
    boolean pnv = generateNullValue != null ? generateNullValue : defaultGenerateNullValue;
    if (!pnv) {
      objectMapper.setSerializationInclusion(Include.NON_NULL);
    }

    try {
      return objectMapper.writeValueAsString(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static byte[] toJsonBytes(Object object) {
    // 优先使用对象属性 generateNullValue，决定转换 json时是否生成 null value
    boolean pnv = generateNullValue != null ? generateNullValue : defaultGenerateNullValue;
    if (!pnv) {
      objectMapper.setSerializationInclusion(Include.NON_NULL);
    }

    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static <T> T parse(String jsonString, Class<T> type) {
    try {
      return objectMapper.readValue(jsonString, type);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static Map<?, ?> parseToMap(String json) {
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    TypeReference<Map<K, V>> typeReference = new TypeReference<Map<K, V>>() {
    };
    try {
      return objectMapper.readValue(json, typeReference);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static Object parseObject(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Object parseArray(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static <T> List<T> parseArray(String str, Class<T> elementType) {
    try {
      return objectMapper.readValue(str, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType) {
    try {
      TypeReference<List<Map<K, V>>> typeReference = new TypeReference<List<Map<K, V>>>() {
      };
      return objectMapper.readValue(stringValue, typeReference);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static Object parse(String stringValue) {
    try {
      return objectMapper.readValue(stringValue, Object.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T parse(String body, Type type) {
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type);
      return objectMapper.readValue(body, javaType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T parse(byte[] body, Type type) {
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    try {
      return objectMapper.readValue(body, javaType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T parse(String body, TioTypeReference<T> tioTypeReference) {
    Type type = tioTypeReference.getType();
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    try {
      return objectMapper.readValue(body, javaType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
