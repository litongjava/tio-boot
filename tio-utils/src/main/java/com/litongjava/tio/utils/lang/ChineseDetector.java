package com.litongjava.tio.utils.lang;

public class ChineseDetector {

  public static boolean isChinese(String text) {
    return isChinese(text, 0.2d);
  }

  /**
   * 判断文本是否为中文（根据中文字符比例）
   * 
   * @param text      输入文本
   * @param threshold 阈值（0-1之间，例如 0.3 表示 30%）
   */
  public static boolean isChinese(String text, double threshold) {
    if (text == null) {
      return false;
    }

    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      return false;
    }

    int chineseCount = 0;
    int totalCount = 0;

    for (int i = 0; i < trimmed.length(); i++) {
      char ch = trimmed.charAt(i);
      if (Character.isWhitespace(ch)) {
        continue;
      }

      totalCount++;
      if (isChineseChar(ch)) {
        chineseCount++;
      }
    }

    if (totalCount == 0) {
      return false;
    }

    double ratio = (double) chineseCount / totalCount;
    return ratio >= threshold;
  }

  private static boolean isChineseChar(char ch) {
    // 判断主 CJK 统一表意字符区
    return (ch >= 0x4E00 && ch <= 0x9FFF);
  }
}
