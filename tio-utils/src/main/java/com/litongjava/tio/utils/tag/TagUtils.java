package com.litongjava.tio.utils.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagUtils {

  /**
   * 提取所有 <suggestions> 标签内的文本内容
   *
   * @param input 包含 <suggestions>...</suggestions> 的原始字符串
   * @return 所有匹配到的标签内容的列表
   */
  public static List<String> extractSuggestions(String input) {
    List<String> results = new ArrayList<>();
    // (?s) 表示开启 DOTALL 模式，使 '.' 匹配包括换行在内的所有字符
    Pattern pattern = Pattern.compile("(?s)<suggestions>(.*?)</suggestions>");
    Matcher matcher = pattern.matcher(input);

    while (matcher.find()) {
      results.add(matcher.group(1).trim());
    }

    return results;
  }

  /**
   * 提取所有 <output> 标签内的文本内容
   *
   * @param input 包含 <output>...</output> 的原始字符串
   * @return 所有匹配到的标签内容的列表
   */
  public static List<String> extractOutput(String input) {
    List<String> results = new ArrayList<>();
    // (?s) 表示开启 DOTALL 模式，使 '.' 匹配包括换行在内的所有字符
    Pattern pattern = null;
    if (input.contains("```xml")) {
      pattern = Pattern.compile("(?s)```xml\\s*<output>(.*?)</output>\\s*```");
    } else {
      pattern = Pattern.compile("(?s)<output>(.*?)</output>");
    }
    Matcher matcher = pattern.matcher(input);

    while (matcher.find()) {
      results.add(matcher.group(1).trim());
    }

    return results;
  }
}
