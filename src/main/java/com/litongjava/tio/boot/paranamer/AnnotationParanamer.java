package com.litongjava.tio.boot.paranamer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.inject.Named;

/**
 * Implementation of Paranamer that uses @Named annotation of JSR 330. It is
 * overridable to allow other annotations to be used (JBehave, Guice's original
 * one)
 *
 * @author Paul Hammant
 */
public class AnnotationParanamer implements Paranamer {

  public static final String __PARANAMER_DATA = "v1.0 \n" + "lookupParameterNames java.lang.AccessibleObject methodOrConstructor \n"
      + "lookupParameterNames java.lang.AccessibleObject,boolean methodOrCtor,throwExceptionIfMissing \n";
  private final Paranamer fallback;

  public AnnotationParanamer() {
    this(new NullParanamer());
  }

  public AnnotationParanamer(Paranamer fallback) {
    this.fallback = fallback;
  }

  public String[] lookupParameterNames(AccessibleObject methodOrConstructor) {
    return lookupParameterNames(methodOrConstructor, true);
  }

  public String[] lookupParameterNames(AccessibleObject methodOrCtor, boolean throwExceptionIfMissing) {
    // Oh for some commonality between Constructor and Method !!
    Class<?>[] types = null;
    Class<?> declaringClass = null;
    String name = null;
    Annotation[][] anns = null;
    if (methodOrCtor instanceof Method) {
      Method method = (Method) methodOrCtor;
      types = method.getParameterTypes();
      name = method.getName();
      declaringClass = method.getDeclaringClass();
      anns = method.getParameterAnnotations();
    } else {
      Constructor<?> constructor = (Constructor<?>) methodOrCtor;
      types = constructor.getParameterTypes();
      declaringClass = constructor.getDeclaringClass();
      name = "<init>";
      anns = constructor.getParameterAnnotations();
    }

    if (types.length == 0) {
      return EMPTY_NAMES;
    }

    final String[] names = new String[types.length];
    boolean allDone = true;
    for (int i = 0; i < names.length; i++) {
      for (int j = 0; j < anns[i].length; j++) {
        Annotation ann = anns[i][j];
        if (isNamed(ann)) {
          names[i] = getNamedValue(ann);
          break;
        }
      }
      if (names[i] == null) {
        allDone = false;
      }

    }

    // fill in blanks from fallback if possible.
    if (!allDone) {
      allDone = true;
      String[] altNames = fallback.lookupParameterNames(methodOrCtor, false);
      if (altNames.length > 0) {
        for (int i = 0; i < names.length; i++) {
          if (names[i] == null) {
            if (altNames[i] != null) {
              names[i] = altNames[i];
            } else {
              allDone = false;
            }
          }
        }
      } else {
        allDone = false;
      }
    }

    // error if applicable
    if (!allDone) {
      if (throwExceptionIfMissing) {
        throw new ParameterNamesNotFoundException("One or more @Named annotations missing for class '" + declaringClass.getName() + "', methodOrCtor "
            + name + " and parameter types " + DefaultParanamer.getParameterTypeNamesCSV(types));
      } else {
        return Paranamer.EMPTY_NAMES;
      }
    }
    return names;
  }

  /**
   * Override this if you want something other than JSR 330's Named annotation.
   *
   * <pre>
   * return ((Named) ann).value();
   * </pre>
   *
   * @param ann the annotation in question
   * @return a the name value.
   */
  protected String getNamedValue(Annotation ann) {
    if ("javax.inject.Named".equals(ann.annotationType().getName())) {
      return Jsr330Helper.getNamedValue(ann);
    } else {
      return null;
    }
  }

  /**
   * Override this if you want something other than JSR 330's Named annotation.
   *
   * <pre>
   * return ann instanceof Named;
   * </pre>
   *
   * @param ann the annotation in question
   * @return whether it is the annotation holding the parameter name
   */
  protected boolean isNamed(Annotation ann) {
    if ("javax.inject.Named".equals(ann.annotationType().getName())) {
      return Jsr330Helper.isNamed(ann);
    } else {
      return false;
    }
  }

  /**
   * This is a different class, because the @Inject jar may not be in the
   * classpath.
   */
  public static class Jsr330Helper {
    private static boolean isNamed(Annotation ann) {
      return ann instanceof Named;
    }

    private static String getNamedValue(Annotation ann) {
      return ((Named) ann).value();
    }

  }

}