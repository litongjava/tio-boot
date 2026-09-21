package nexus.io.tio.utils.map;

import java.util.LinkedHashMap;
import java.util.Map;

import nexus.io.tio.utils.name.CamelNameUtils;

public class MapUtils {
  /** Ordered map builder accepting null values. */
  public static Map<String, Object> of(Object... pairs) {
    if (pairs == null || pairs.length % 2 != 0) throw new IllegalArgumentException("Key/value pairs required");
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) {
      if (!(pairs[i] instanceof String)) throw new IllegalArgumentException("Keys must be strings");
      result.put((String) pairs[i], pairs[i + 1]);
    }
    return result;
  }
  public static <T> Map<String, T> camelToUnderscore(Map<String, T> map) {
    Map<String, T> result = new LinkedHashMap<>(map.size());
    for (Map.Entry<String, T> entry : map.entrySet()) {
      String newKey = CamelNameUtils.toUnderscore(entry.getKey());
      result.put(newKey, entry.getValue());
    }
    return result;
  }
}
