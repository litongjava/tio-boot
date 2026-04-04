package nexus.io.tio.boot.utils;

import java.lang.reflect.Method;

import nexus.io.tio.boot.paranamer.BytecodeReadingParanamer;
import nexus.io.tio.boot.paranamer.Paranamer;

public class ParameterNameUtil {
  private static final Paranamer paranamer;

  static {
    paranamer = new BytecodeReadingParanamer();
  }

  public static String[] getParameterNames(Method method) {
    return paranamer.lookupParameterNames(method, false);
  }
}
