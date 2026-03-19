package com.litongjava.tio.boot.paranamer;

import java.lang.reflect.AccessibleObject;

/**
 * Implementation of Paranamer which adheres to the NullObject pattern
 *
 * @author Paul Hammant
 */
public class NullParanamer implements Paranamer {

  public String[] lookupParameterNames(AccessibleObject methodOrConstructor) {
    return new String[0];
  }

  public String[] lookupParameterNames(AccessibleObject methodOrConstructor, boolean throwExceptionIfMissing) {
    if (throwExceptionIfMissing) {
      throw new ParameterNamesNotFoundException("NullParanamer implementation predictably finds no parameter names");
    }
    return Paranamer.EMPTY_NAMES;
  }
}