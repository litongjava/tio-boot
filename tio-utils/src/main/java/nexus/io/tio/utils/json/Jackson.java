package nexus.io.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import nexus.io.model.type.TioTypeReference;
import nexus.io.tio.utils.date.TioTimeUtils;

/**
 * Json 转换 jackson 实现.
 */
@SuppressWarnings("deprecation")
public class Jackson extends Json {

  // Jackson 默认是否输出 null
  private static boolean defaultGenerateNullValue = true;

  // 对象级配置，优先级高于 defaultGenerateNullValue
  protected Boolean writeNulls = null;

  protected static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

    // 没有 getter 方法时不抛异常
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  public Jackson() {
    this.writeNulls = true;
  }

  public Jackson(boolean writeNulls) {
    this.writeNulls = writeNulls;
  }

  public static void setDefaultGenerateNullValue(boolean defaultGenerateNullValue) {
    Jackson.defaultGenerateNullValue = defaultGenerateNullValue;
  }

  public Jackson setGenerateNullValue(boolean generateNullValue) {
    this.writeNulls = generateNullValue;
    return this;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static Jackson getJson() {
    return new Jackson();
  }

  private ObjectWriter buildWriter() {
    boolean pnv = writeNulls != null ? writeNulls : defaultGenerateNullValue;

    ObjectMapper mapper = objectMapper.copy();

    if (pnv) {
      mapper.setSerializationInclusion(Include.ALWAYS);
    } else {
      mapper.setSerializationInclusion(Include.NON_NULL);
    }

    String dp = datePattern != null ? datePattern : getDefaultDatePattern();
    if (dp != null) {
      mapper.setDateFormat(TioTimeUtils.getSimpleDateFormat(dp));
    }

    return mapper.writer();
  }

  @Override
  public String toJson(Object object) {
    try {
      return buildWriter().writeValueAsString(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
  public byte[] toJsonBytes(Object object) {
    try {
      return buildWriter().writeValueAsBytes(object);
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
    }
  }

  @Override
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
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object parseArray(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> List<T> parseArray(String str, Class<T> elementType) {
    try {
      return objectMapper.readValue(str,
          objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
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