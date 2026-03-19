package com.litongjava.tio.utils.hutool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataMaskingUtil {

  /**
   * 脱敏姓名
   * 例如：张三 -> 张*
   *
   * @param name 原始姓名
   * @return 脱敏后的姓名
   */
  public static String maskName(String name) {
    if (name == null || name.isEmpty()) {
      return name;
    }
    if (name.length() == 1) {
      return "*";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(name.charAt(0));
    for (int i = 1; i < name.length(); i++) {
      sb.append("*");
    }
    return sb.toString();
  }

  /**
   * 脱敏邮箱
   * 例如：user@example.com -> u***@example.com
   *
   * @param email 原始邮箱
   * @return 脱敏后的邮箱
   */
  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return email;
    }
    int atIndex = email.indexOf("@");
    if (atIndex <= 1) { // 如果用户名部分太短，直接全部替换为*
      return email.replaceAll(".", "*");
    }
    StringBuilder sb = new StringBuilder();
    sb.append(email.charAt(0));
    for (int i = 1; i < atIndex; i++) {
      sb.append("*");
    }
    sb.append(email.substring(atIndex));
    return sb.toString();
  }

  /**
   * 脱敏电话号码
   * 例如：13812345678 -> 138****5678
   *
   * @param phone 原始电话号码
   * @return 脱敏后的电话号码
   */
  public static String maskPhone(String phone) {
    if (phone == null || phone.isEmpty()) {
      return phone;
    }
    // 支持国内常见的11位手机号
    return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
  }

  /**
   * 脱敏身份证号
   * 例如：123456789012345678 -> 123456********5678
   *
   * @param idCard 原始身份证号
   * @return 脱敏后的身份证号
   */
  public static String maskIdCard(String idCard) {
    if (idCard == null || idCard.isEmpty()) {
      return idCard;
    }
    // 保留前6位和后4位，中间用*替代
    if (idCard.length() < 10) {
      // 如果长度不足10位，全部替换为*
      return idCard.replaceAll(".", "*");
    }
    StringBuilder sb = new StringBuilder();
    sb.append(idCard.substring(0, 6));
    for (int i = 6; i < idCard.length() - 4; i++) {
      sb.append("*");
    }
    sb.append(idCard.substring(idCard.length() - 4));
    return sb.toString();
  }

  /**
   * 脱敏信用卡号
   * 例如：1234 5678 9012 3456 -> 1234 **** **** 3456
   *
   * @param creditCard 原始信用卡号
   * @return 脱敏后的信用卡号
   */
  public static String maskCreditCard(String creditCard) {
    if (creditCard == null || creditCard.isEmpty()) {
      return creditCard;
    }
    // 移除所有空格
    String sanitized = creditCard.replaceAll("\\s+", "");
    if (sanitized.length() < 8) {
      // 如果长度不足8位，全部替换为*
      return sanitized.replaceAll(".", "*");
    }
    StringBuilder sb = new StringBuilder();
    sb.append(sanitized.substring(0, 4));
    sb.append(" **** **** ");
    sb.append(sanitized.substring(sanitized.length() - 4));
    return sb.toString();
  }

  /**
   * 脱敏 API 密钥
   * 例如：sk-xxxxaG9 -> sk-Mt7n*************seaG9
   *
   * @param apiKey 原始 API 密钥
   * @return 脱敏后的 API 密钥
   */
  public static String maskApiKey(String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) {
      return apiKey;
    }
    // 保留前4位和后4位，中间用*代替
    // 调整保留的位数根据实际需求
    Pattern pattern = Pattern.compile("^([A-Za-z0-9]{7})([A-Za-z0-9]+)([A-Za-z0-9]{4})$");
    Matcher matcher = pattern.matcher(apiKey);
    if (matcher.matches()) {
      String prefix = matcher.group(1);
      String maskedMiddle = matcher.group(2).replaceAll(".", "*");
      String suffix = matcher.group(3);
      return prefix + maskedMiddle + suffix;
    } else {
      // 如果不符合预期格式，进行通用脱敏处理，保留前4位和后4位
      if (apiKey.length() <= 8) {
        return apiKey.replaceAll(".", "*");
      }
      String prefix = apiKey.substring(0, 4);
      String suffix = apiKey.substring(apiKey.length() - 4);
      StringBuilder sb = new StringBuilder(prefix);
      for (int i = 4; i < apiKey.length() - 4; i++) {
        sb.append("*");
      }
      sb.append(suffix);
      return sb.toString();
    }
  }

  public static String maskStr(String text) {
    return maskByRegex(text, "(\\d{3})\\d{4}(\\d{4})", "$1****$2");
  }

  /**
   * 通用脱敏方法
   * 根据正则表达式进行脱敏处理
   *
   * @param original 原始字符串
   * @param regex    匹配模式
   * @param replacement 替换字符串
   * @return 脱敏后的字符串
   */
  public static String maskByRegex(String original, String regex, String replacement) {
    if (original == null || original.isEmpty()) {
      return original;
    }
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(original);
    return matcher.replaceAll(replacement);
  }
}
