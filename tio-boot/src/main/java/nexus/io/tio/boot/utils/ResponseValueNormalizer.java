package nexus.io.tio.boot.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jfinal.kit.Kv;
import nexus.io.tio.utils.json.JsonUtils;

/** Explicit response conversion; does not change the application's global JSON policy. */
public final class ResponseValueNormalizer {
  private ResponseValueNormalizer() {
  }

  public static Object normalize(Object value) {
    if (value instanceof Map<?, ?>) {
      Kv result = Kv.create();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        result.set(String.valueOf(entry.getKey()), normalize(entry.getValue()));
      }
      return result;
    }
    if (value instanceof List<?>) {
      List<Object> result = new ArrayList<>();
      for (Object item : (List<?>) value) {
        result.add(normalize(item));
      }
      return result;
    }
    if (value instanceof Timestamp) {
      return ((Timestamp) value).toInstant().toString();
    }
    if (value instanceof Long) {
      return value.toString();
    }
    if (value == null) {
      return null;
    }
    // Keep the PostgreSQL driver optional and decode only JSON/JSONB objects.
    for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
      if ("org.postgresql.util.PGobject".equals(type.getName())) {
        try {
          String pgType = (String) type.getMethod("getType").invoke(value);
          String text = (String) type.getMethod("getValue").invoke(value);
          if ("json".equalsIgnoreCase(pgType) || "jsonb".equalsIgnoreCase(pgType)) {
            return text == null ? null : normalize(JsonUtils.parse(text, Object.class));
          }
          return value;
        } catch (ReflectiveOperationException error) {
          throw new IllegalStateException("Cannot read PostgreSQL value", error);
        }
      }
    }
    Class<?> parent = value.getClass().getSuperclass();
    if (parent != null && "java.lang.Record".equals(parent.getName())) {
      Kv result = Kv.create();
      try {
        // Reflection preserves Java 8 compatibility while supporting newer record models.
        Object[] components = (Object[]) Class.class.getMethod("getRecordComponents").invoke(value.getClass());
        for (Object component : components) {
          String name = (String) component.getClass().getMethod("getName").invoke(component);
          Method accessor = (Method) component.getClass().getMethod("getAccessor").invoke(component);
          for (Annotation annotation : accessor.getAnnotations()) {
            if ("com.alibaba.fastjson2.annotation.JSONField".equals(annotation.annotationType().getName())) {
              String alias = (String) annotation.annotationType().getMethod("name").invoke(annotation);
              if (!alias.isEmpty()) {
                name = alias;
              }
            }
          }
          result.set(name, normalize(accessor.invoke(value)));
        }
      } catch (ReflectiveOperationException error) {
        throw new IllegalStateException("Cannot normalize response record", error);
      }
      return result;
    }
    return value;
  }
}
