package com.litongjava.tio.utils.lang;

public class ChineseUtils {
  /**
   * 判断文本中是否包含中文字符
   * 修正后的正则表达式包含基本汉字和扩展A区
   *
   * @param text 输入的文本
   * @return 如果包含中文字符则返回 true，否则返回 false
   */
  public static boolean containsChinese(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    // 扩展正则表达式以包含更多汉字区域
    return text.matches(".*[\\u4E00-\\u9FA5\\u3400-\\u4DBF\\uF900-\\uFAFF]+.*");
  }
}
