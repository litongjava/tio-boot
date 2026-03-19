package com.litongjava.tio.utils.http.useragent;

import com.litongjava.model.http.useragent.UserAgent;

/**
 * User-Agent工具类
 * 
 * @author looly
 *
 */
public class UserAgentUtil {

  /**
   * 解析User-Agent
   * 
   * @param userAgentString User-Agent字符串
   * @return {@link UserAgent}
   */
  public static UserAgent parse(String userAgentString) {
    return UserAgentParser.parse(userAgentString);
  }

  public static boolean isWechat(String userAgentString) {
    return userAgentString.contains("WeChat/");
  }

  public static boolean isQQ(String userAgentString) {
    return userAgentString.contains("QQ/");
  }
}