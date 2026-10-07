package nexus.io.tio.utils.validator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Validate record input shapes before JSON binding can coerce scalar values. */
public final class RecordParameterValidator {
  private RecordParameterValidator() {
  }

  public static void validate(Map<?, ?> values, Class<?> type) {
    ParameterValidator.require(values != null, "Request parameters are required");
    Class<?> parent = type.getSuperclass();
    if (parent == null || !"java.lang.Record".equals(parent.getName())) {
      return;
    }
    try {
      Object[] components = (Object[]) Class.class.getMethod("getRecordComponents").invoke(type);
      for (Object component : components) {
        String name = (String) component.getClass().getMethod("getName").invoke(component);
        Type fieldType = (Type) component.getClass().getMethod("getGenericType").invoke(component);
        ParameterValidator.require(valid(values.get(name), fieldType), name + " has an invalid type");
      }
    } catch (ReflectiveOperationException error) {
      throw new IllegalStateException("Cannot inspect request record", error);
    }
  }

  private static boolean valid(Object value, Type type) {
    if (value == null) {
      return !(type instanceof Class<?> && ((Class<?>) type).isPrimitive());
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType generic = (ParameterizedType) type;
      Type[] arguments = generic.getActualTypeArguments();
      if (generic.getRawType() == List.class && value instanceof List<?>) {
        for (Object item : (List<?>) value) {
          if (item == null || !valid(item, arguments[0])) {
            return false;
          }
        }
        return true;
      }
      if (generic.getRawType() == Map.class && value instanceof Map<?, ?>) {
        for (Map.Entry<?, ?> item : ((Map<?, ?>) value).entrySet()) {
          if (!valid(item.getKey(), arguments[0]) || !valid(item.getValue(), arguments[1])) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
    if (!(type instanceof Class<?>)) {
      return false;
    }
    Class<?> expected = (Class<?>) type;
    Class<?> parent = expected.getSuperclass();
    if (parent != null && "java.lang.Record".equals(parent.getName()) && value instanceof Map<?, ?>) {
      validate((Map<?, ?>) value, expected);
      return true;
    }
    if (Map.class.isAssignableFrom(expected)) {
      return value instanceof Map<?, ?>;
    }
    if (expected == Long.class || expected == long.class || expected == Integer.class || expected == int.class) {
      if (!(value instanceof Number)) {
        return false;
      }
      try {
        BigDecimal number = new BigDecimal(value.toString());
        if (expected == Long.class || expected == long.class) {
          number.longValueExact();
        } else {
          number.intValueExact();
        }
        return true;
      } catch (ArithmeticException | NumberFormatException error) {
        return false;
      }
    }
    if (expected == boolean.class) {
      return value instanceof Boolean;
    }
    return expected.isInstance(value);
  }
}
