package com.litongjava.tio.boot.paranamer;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class PositionalParanamer implements Paranamer {

  private final String prefix;

  /**
   * Default Contstructor with prefix <code>arg</code>.
   */
  public PositionalParanamer() {
    this("arg");
  }

  /**
   * Constructor that allows to override the prefix.
   * 
   * @param prefix string that is prepended before the position of the parameter.
   */
  public PositionalParanamer(String prefix) {
    super();
    this.prefix = prefix;
  }

  public String[] lookupParameterNames(AccessibleObject methodOrConstructor) {
    return lookupParameterNames(methodOrConstructor, true);
  }

  public String[] lookupParameterNames(AccessibleObject methodOrCtor, boolean throwExceptionIfMissing) {
    int count = count(methodOrCtor);
    String[] result = new String[count];
    for (int i = 0; i < result.length; i++) {
      result[i] = prefix + i;
    }
    return result;
  }

  private int count(AccessibleObject methodOrCtor) {
    if (methodOrCtor instanceof Method) {
      Method method = (Method) methodOrCtor;
      return method.getParameterTypes().length;
    }
    Constructor<?> constructor = (Constructor<?>) methodOrCtor;
    return constructor.getParameterTypes().length;
  }

}
