package com.litongjava.tio.utils.validator;

import org.junit.Test;

public class PasswordValidatorTest {

  @Test
  public void test() {
      // 测试示例
      String password1 = "Aa1!23";
      String password2 = "123456";
      String password3 = "Aa1234";
      String password4 = "Aa!@";

      System.out.println("Password 1: " + password1 + " -> " + (PasswordValidator.validate(password1) ? "Valid" : "Invalid"));
      System.out.println("Password 2: " + password2 + " -> " + (PasswordValidator.validate(password2) ? "Valid" : "Invalid"));
      System.out.println("Password 3: " + password3 + " -> " + (PasswordValidator.validate(password3) ? "Valid" : "Invalid"));
      System.out.println("Password 4: " + password4 + " -> " + (PasswordValidator.validate(password4) ? "Valid" : "Invalid"));
  }

}
