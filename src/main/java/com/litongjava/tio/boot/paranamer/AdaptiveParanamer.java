package com.litongjava.tio.boot.paranamer;

import java.lang.reflect.AccessibleObject;

/**
 * Implementation of Paranamer which chooses between a series of Paranamer
 * instances depending on which can supply data. It prioritizes the paranamer
 * instances according to the order they were passed in.
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 */
public class AdaptiveParanamer implements Paranamer {

  public static final String __PARANAMER_DATA = "v1.0 \n"
      + "com.thoughtworks.paranamer.AdaptiveParanamer AdaptiveParanamer com.thoughtworks.paranamer.Paranamer,com.thoughtworks.paranamer.Paranamer delegate,fallback\n"
      + "com.thoughtworks.paranamer.AdaptiveParanamer AdaptiveParanamer com.thoughtworks.paranamer.Paranamer,com.thoughtworks.paranamer.Paranamer,com.thoughtworks.paranamer.Paranamer delegate,fallback,reserve\n"
      + "com.thoughtworks.paranamer.AdaptiveParanamer AdaptiveParanamer com.thoughtworks.paranamer.Paranamer[] paranamers\n"
      + "com.thoughtworks.paranamer.AdaptiveParanamer lookupParameterNames java.lang.AccessibleObject methodOrConstructor \n"
      + "com.thoughtworks.paranamer.AdaptiveParanamer lookupParameterNames java.lang.AccessibleObject,boolean methodOrCtor,throwExceptionIfMissing \n";

  private final Paranamer[] paranamers;

  /**
   * Use DefaultParanamer ahead of BytecodeReadingParanamer
   */
  public AdaptiveParanamer() {
    this(new DefaultParanamer(), new BytecodeReadingParanamer());
  }

  /**
   * Prioritize a series of Paranamers
   * 
   * @param paranamers the paranamers in question
   */
  public AdaptiveParanamer(Paranamer... paranamers) {
    this.paranamers = paranamers;
  }

  public String[] lookupParameterNames(AccessibleObject methodOrConstructor) {
    return lookupParameterNames(methodOrConstructor, true);
  }

  public String[] lookupParameterNames(AccessibleObject methodOrCtor, boolean throwExceptionIfMissing) {
    for (int i = 0; i < paranamers.length; i++) {
      Paranamer paranamer = paranamers[i];
      String[] names = paranamer.lookupParameterNames(methodOrCtor, i + 1 < paranamers.length ? false : throwExceptionIfMissing);
      if (names != Paranamer.EMPTY_NAMES) {
        return names;
      }
    }
    return Paranamer.EMPTY_NAMES;
  }

}