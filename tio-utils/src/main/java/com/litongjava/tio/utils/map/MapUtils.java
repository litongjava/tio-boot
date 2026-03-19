package com.litongjava.tio.utils.map;

import java.util.LinkedHashMap;
import java.util.Map;

import com.litongjava.tio.utils.name.CamelNameUtils;

public class MapUtils {
  public static <T> Map<String, T> camelToUnderscore(Map<String, T> map) {
    Map<String, T> result = new LinkedHashMap<>(map.size());
    for (Map.Entry<String, T> entry : map.entrySet()) {
      String newKey = CamelNameUtils.toUnderscore(entry.getKey());
      result.put(newKey, entry.getValue());
    }
    return result;
  }
}
