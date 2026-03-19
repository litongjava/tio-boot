package com.litongjava.tio.utils;

import java.util.Collection;
import java.util.List;

public class AppendJsonConverter {

  /**
   * 将 List<Long> 转换为 JSON 字符串。
   * 
   * @param list 需要转换的 List<Long>
   * @return 转换后的 JSON 字符串
   */
  public static String convertListLongToJson(List<Long> list) {
    if (list == null || list.isEmpty()) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    // 遍历列表，将每个元素添加到 JSON 字符串中
    for (int i = 0; i < list.size(); i++) {
      sb.append(list.get(i));
      if (i < list.size() - 1) {
        sb.append(", ");
      }
    }

    sb.append("]");

    return sb.toString();
  }

  /**
   * 将 Collection<String> 转为 JSON 字符串。
   * 
   * @param collection 需要转换的 Collection<String>
   * @return 转换后的 JSON 字符串
   */
  public static String convertCollectionStringToJson(Collection<String> collection) {
    if (collection == null || collection.isEmpty()) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    // 遍历集合，将每个字符串元素添加到 JSON 字符串中
    boolean isFirst = true;
    for (String element : collection) {
      if (!isFirst) {
        sb.append(", ");
      }
      sb.append("\"").append(escapeJson(element)).append("\"");
      isFirst = false;
    }

    sb.append("]");

    return sb.toString();
  }

  /**
   * 对字符串中的特殊字符进行转义，以符合 JSON 标准。
   * 
   * @param s 需要转义的字符串
   * @return 转义后的字符串
   */
  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\") //
        .replace("\"", "\\\"") //
        .replace("\b", "\\b") //
        .replace("\f", "\\f") //
        .replace("\n", "\\n") //
        .replace("\r", "\\r") //
        .replace("\t", "\\t");//
  }
}
