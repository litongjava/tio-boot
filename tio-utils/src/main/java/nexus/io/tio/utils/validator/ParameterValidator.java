package nexus.io.tio.utils.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Stateless validation/conversion for values from HTTP, files or external APIs. */
public final class ParameterValidator {
  private ParameterValidator() { }

  public static void require(boolean valid, String message) {
    if (!valid) throw new ParameterValidationException(message);
  }

  public static String text(Object raw, String name, int max, boolean required) {
    require(raw == null || raw instanceof String, name + " must be a string");
    String value = raw == null ? null : ((String) raw).trim();
    require(!required || value != null && !value.isEmpty(), name + " is required");
    require(value == null || value.length() <= max, name + " is too long");
    return value == null || value.isEmpty() ? null : value;
  }

  public static String text(Object raw, String name, int max) {
    return text(raw, name, max, true);
  }

  public static long longValue(Object raw, String name, long min, long max, Long fallback) {
    if (raw == null && fallback != null) return checked(fallback, name, min, max);
    String value = String.valueOf(raw);
    require(value.matches("-?[0-9]+"), name + " must be an integer");
    try {
      return checked(Long.parseLong(value), name, min, max);
    } catch (NumberFormatException e) {
      throw new ParameterValidationException(name + " is invalid");
    }
  }

  private static long checked(long value, String name, long min, long max) {
    require(value >= min && value <= max, name + " is out of range");
    return value;
  }

  public static long id(Object value, String name) {
    return longValue(value, name, 1, Long.MAX_VALUE, null);
  }

  public static String choice(Object raw, String name, String fallback, String... options) {
    String value = text(raw, name, 64, false);
    if (value == null) value = fallback;
    require(Arrays.asList(options).contains(value), name + " is invalid");
    return value;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> object(Object raw, String name) {
    if (raw == null) return Collections.emptyMap();
    require(raw instanceof Map, name + " must be an object");
    for (Object key : ((Map<?, ?>) raw).keySet()) require(key instanceof String, name + " must have string keys");
    return (Map<String, Object>) raw;
  }

  @SuppressWarnings("unchecked")
  public static List<Object> array(Object raw, String name, int max) {
    if (raw == null) return Collections.emptyList();
    require(raw instanceof List && ((List<?>) raw).size() <= max, name + " must be a bounded array");
    return (List<Object>) raw;
  }
}
