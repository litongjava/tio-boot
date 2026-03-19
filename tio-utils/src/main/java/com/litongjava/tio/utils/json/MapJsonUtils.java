package com.litongjava.tio.utils.json;

import java.util.Iterator;
import java.util.Map;

public class MapJsonUtils {

  /**
   * Convert a Map to a pretty JSON string manually.
   *
   * @param map the map to be converted to JSON
   * @return a pretty-printed JSON representation of the map
   */
  public static String toPrettyJson(Map<?, ?> map) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    Iterator<?> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
      sb.append("  \"").append(escapeString(entry.getKey().toString())).append("\": ").append("\"")
          .append(escapeString(entry.getValue().toString())).append("\"");
      if (iter.hasNext()) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Escape string to be JSON-compliant.
   *
   * @param str the string to be escaped
   * @return the escaped JSON string
   */
  private static String escapeString(String str) {
    return str.replace("\\", "\\\\")//
        .replace("\"", "\\\"")//
        .replace("\b", "\\b")//
        .replace("\f", "\\f")//
        .replace("\n", "\\n")//
        .replace("\r", "\\r")//
        .replace("\t", "\\t");
  }
}
