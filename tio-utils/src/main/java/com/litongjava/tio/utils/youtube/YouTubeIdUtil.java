package com.litongjava.tio.utils.youtube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeIdUtil {

  // 改进后的正则表达式，支持各种 URL 格式
  private static final Pattern YOUTUBE_PATTERN = Pattern.compile("^(?:https?://)?(?:www\\.)?(?:youtu\\.be/|youtube\\.com/(?:(?:watch\\?(?:.*&)?v=)|(?:embed/)))([a-zA-Z0-9_-]{11})");

  /**
   * 提取给定 YouTube URL 中的视频 ID.
   *
   * @param url 包含 YouTube 视频地址的字符串
   * @return 视频 ID 字符串，如果无法提取则返回 null
   */
  public static String extractVideoId(String url) {
    if (url == null || url.trim().isEmpty()) {
      return null;
    }
    Matcher matcher = YOUTUBE_PATTERN.matcher(url);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  // 测试方法
  public static void main(String[] args) {
    String[] urls = { "https://www.youtube.com/watch?v=HVV4Y6kO2Es", "https://www.youtube.com/embed/HVV4Y6kO2Es", "https://youtu.be/HVV4Y6kO2Es" };

    for (String url : urls) {
      System.out.println("URL: " + url);
      System.out.println("视频ID: " + extractVideoId(url));
      System.out.println("---------");
    }
  }
}
