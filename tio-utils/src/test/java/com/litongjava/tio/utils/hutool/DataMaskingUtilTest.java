package com.litongjava.tio.utils.hutool;

import org.junit.Test;

public class DataMaskingUtilTest {

  @Test
  public void test() {
    // 测试姓名脱敏
    String name = "张三丰";
    System.out.println("原姓名: " + name + " -> 脱敏后: " + DataMaskingUtil.maskName(name));

    // 测试邮箱脱敏
    String email = "user@example.com";
    System.out.println("原邮箱: " + email + " -> 脱敏后: " + DataMaskingUtil.maskEmail(email));

    // 测试电话号码脱敏
    String phone = "13812345678";
    System.out.println("原电话: " + phone + " -> 脱敏后: " + DataMaskingUtil.maskPhone(phone));

    // 测试身份证号脱敏
    String idCard = "123456789012345678";
    System.out.println("原身份证: " + idCard + " -> 脱敏后: " + DataMaskingUtil.maskIdCard(idCard));

    // 测试信用卡号脱敏
    String creditCard = "1234 5678 9012 3456";
    System.out.println("原信用卡: " + creditCard + " -> 脱敏后: " + DataMaskingUtil.maskCreditCard(creditCard));

    // 测试通用脱敏方法
    String text = "敏感信息: user@example.com, 电话: 13812345678";
    String maskedText = DataMaskingUtil.maskByRegex(text, "(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    System.out.println("原文本: " + text + " -> 脱敏后: " + maskedText);

    String maskStr = DataMaskingUtil.maskApiKey("sk-Mt7nRKZgvxxxxxxxxVTY5seaG9");
    System.out.println(maskStr);
  }

}
