package com.litongjava.tio.boot.utils;

import java.lang.reflect.Method;

import com.litongjava.tio.boot.paranamer.BytecodeReadingParanamer;
import com.litongjava.tio.boot.paranamer.Paranamer;

public class ParameterNameUtil {
  private static final Paranamer paranamer;

  static {
    paranamer = new BytecodeReadingParanamer();
  }

  public static String[] getParameterNames(Method method) {
    return paranamer.lookupParameterNames(method, false);
  }
}
