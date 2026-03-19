package com.litongjava.tio.utils.validator;

import org.junit.Test;

public class EmailValidatorTest {

  @Test
  public void test() {
    // 测试示例
    String email1 = "example@example.com";
    String email2 = "user.name@domain.co";
    String email3 = "user@domain";
    String email4 = "user@domain.corporate";

    System.out.println("Email 1: " + email1 + " -> " + (EmailValidator.validate(email1) ? "Valid" : "Invalid"));
    System.out.println("Email 2: " + email2 + " -> " + (EmailValidator.validate(email2) ? "Valid" : "Invalid"));
    System.out.println("Email 3: " + email3 + " -> " + (EmailValidator.validate(email3) ? "Valid" : "Invalid"));
    System.out.println("Email 4: " + email4 + " -> " + (EmailValidator.validate(email4) ? "Valid" : "Invalid"));
  }

}
