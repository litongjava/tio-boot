package com.litongjava.tio.utils.validator;

import java.util.regex.Pattern;

public class PasswordValidator {

  private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");
  private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

  public static boolean validate(String password) {
    if (password == null || password.length() < 6) {
      return false;
    }

    int criteriaCount = 0;

    if (UPPER_CASE_PATTERN.matcher(password).find()) {
      criteriaCount++;
    }
    if (LOWER_CASE_PATTERN.matcher(password).find()) {
      criteriaCount++;
    }
    if (DIGIT_PATTERN.matcher(password).find()) {
      criteriaCount++;
    }
    if (SPECIAL_CHAR_PATTERN.matcher(password).find()) {
      criteriaCount++;
    }

    return criteriaCount >= 3;
  }
}
