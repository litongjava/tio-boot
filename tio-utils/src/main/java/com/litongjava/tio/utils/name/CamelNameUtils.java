package com.litongjava.tio.utils.name;

public class CamelNameUtils {
  /**
   * 驼峰转下划线
   * @param camelCase
   * @return
   */
  public static String toUnderscore(String camelCase) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < camelCase.length(); i++) {
      char c = camelCase.charAt(i);
      if (Character.isUpperCase(c)) {
        sb.append("_").append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static String toCamel(String underscore) {
    StringBuilder sb = new StringBuilder();
    String[] parts = underscore.split("_");
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (i == 0) {
        sb.append(part);
      } else {
        sb.append(Character.toUpperCase(part.charAt(0)));
        if (part.length() > 1) {
          sb.append(part.substring(1));
        }
      }
    }
    return sb.toString();
  }

}
