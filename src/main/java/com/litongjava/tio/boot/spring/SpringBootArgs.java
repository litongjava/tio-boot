package com.litongjava.tio.boot.spring;

public class SpringBootArgs {

  private static Class<?> primarySource = null;
  private static String[] args = null;

  public static void set(Class<?> primarySource, String[] args) {
    SpringBootArgs.primarySource = primarySource;
    SpringBootArgs.args = args;
  }

  public static Class<?> getPrimarySource() {
    return primarySource;
  }

  public static String[] getArgs() {
    return args;
  }
}
