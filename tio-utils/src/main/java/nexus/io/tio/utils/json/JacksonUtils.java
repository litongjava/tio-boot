package nexus.io.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import nexus.io.model.type.TioTypeReference;

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

  // Global default used when no explicit null-output override is configured.
  private static volatile boolean defaultGenerateNullValue = true;

  // Explicit null-output policy; null delegates to the global default.
  protected static volatile Boolean generateNullValue = null;

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

  private static ObjectWriter buildWriter() {
    Boolean override = generateNullValue;
    boolean writeNulls = override != null ? override : defaultGenerateNullValue;
    // Per-call configuration must not mutate the shared mapper or its serializer cache.
    ObjectMapper mapper = objectMapper.copy();
    mapper.setSerializationInclusion(writeNulls ? Include.ALWAYS : Include.NON_NULL);
    return mapper.writer();
  }

  public static String toJson(Object object) {
    try {
      return buildWriter().writeValueAsString(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  public static byte[] toJsonBytes(Object object) {
    try {
      return buildWriter().writeValueAsBytes(object);
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
    JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, kType, vType);
    try {
      return objectMapper.readValue(json, mapType);
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
      JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, kType, vType);
      JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, mapType);
      return objectMapper.readValue(stringValue, listType);
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
