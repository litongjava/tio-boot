package com.litongjava.tio.boot.paranamer;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Paranamer allows lookups of methods and constructors by parameter names.
 * 
 * @author Paul Hammant
 * @author Mauro Talevi
 */
public interface Paranamer {

  static final String[] EMPTY_NAMES = new String[0];

  /**
   * Lookup the parameter names of a given method.
   * 
   * @param methodOrConstructor the {@link Method} or {@link Constructor} for
   *                            which the parameter names are looked up.
   * @return A list of the parameter names.
   * @throws ParameterNamesNotFoundException if no parameter names were found.
   * @throws NullPointerException            if the parameter is null.
   * @throws SecurityException               if reflection is not permitted on the
   *                                         containing {@link Class} of the
   *                                         parameter
   */
  public String[] lookupParameterNames(AccessibleObject methodOrConstructor);

  /**
   * Lookup the parameter names of a given method.
   *
   * @param methodOrConstructor     the {@link Method} or {@link Constructor} for
   *                                which the parameter names are looked up.
   * @param throwExceptionIfMissing whether to throw an exception if no Paranamer
   *                                data found (versus return null).
   * @return A list of the parameter names.
   * @throws ParameterNamesNotFoundException if no parameter names were found.
   * @throws NullPointerException            if the parameter is null.
   * @throws SecurityException               if reflection is not permitted on the
   *                                         containing {@link Class} of the
   *                                         parameter
   */
  public String[] lookupParameterNames(AccessibleObject methodOrConstructor, boolean throwExceptionIfMissing);

}
