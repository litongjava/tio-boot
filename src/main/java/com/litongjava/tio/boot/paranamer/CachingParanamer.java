package com.litongjava.tio.boot.paranamer;

import java.lang.reflect.AccessibleObject;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Paranamer which delegate to another Paranamer
 * implementation, adding caching functionality to speed up usage. It also uses
 * a WeakHashmap as an implementation detail (wrapped in
 * Collections.synchronizedMap(..)), to allow large usages to garbage collect
 * things as big as whole classloaders (after working through all the refs that
 * originated from that classloader). Tomcat and other 'containers' do this
 * during hot application deployment, undeployment and most importantly for
 * Paranamer redeployment. Basically, this will allow a perm-gen usage keeps
 * growing scenario.
 * 
 * @author Paul Hammant
 * @author Mauro Talevi
 */
public class CachingParanamer implements Paranamer {

  public static final String __PARANAMER_DATA = "v1.0 \n"
      + "com.thoughtworks.paranamer.CachingParanamer <init> com.thoughtworks.paranamer.Paranamer delegate \n"
      + "com.thoughtworks.paranamer.CachingParanamer lookupParameterNames java.lang.AccessibleObject methodOrConstructor \n"
      + "com.thoughtworks.paranamer.CachingParanamer lookupParameterNames java.lang.AccessibleObject, boolean methodOrCtor,throwExceptionIfMissing \n";

  private final Paranamer delegate;

  private final Map<AccessibleObject, String[]> methodCache = makeMethodCache();

  protected Map<AccessibleObject, String[]> makeMethodCache() {
    return Collections.synchronizedMap(new WeakHashMap<AccessibleObject, String[]>());
  }

  /**
   * Uses a DefaultParanamer as the implementation it delegates to.
   */
  public CachingParanamer() {
    this(new DefaultParanamer());
  }

  /**
   * Specify a Paranamer instance to delegates to.
   * 
   * @param delegate the paranamer instance to use
   */
  public CachingParanamer(Paranamer delegate) {
    this.delegate = delegate;
  }

  public String[] lookupParameterNames(AccessibleObject methodOrConstructor) {
    return lookupParameterNames(methodOrConstructor, true);
  }

  public String[] lookupParameterNames(AccessibleObject methodOrCtor, boolean throwExceptionIfMissing) {
    String[] names = methodCache.get(methodOrCtor);
    // refer PARANAMER-19
    if (names == null) {
      names = delegate.lookupParameterNames(methodOrCtor, throwExceptionIfMissing);
      methodCache.put(methodOrCtor, names);
    }
    return names;
  }

  /**
   * This implementation has a better concurrent design (ConcurrentHashMap) which
   * has a better strategy to implement concurrency: segments instead of
   * synchronized.
   *
   * It also drops the underlying WeakHashMap implementation as that can't work
   * with ConcurrentHashMap with some risk of growing permgen for a certain class
   * of usage.
   *
   * So instead of wrapping via 'Collections.synchronizedMap(new WeakHashMap())'
   * we now have 'new ConcurrentHashMap()'
   *
   */
  public static class WithoutWeakReferences extends CachingParanamer {

    public WithoutWeakReferences() {
    }

    public WithoutWeakReferences(Paranamer delegate) {
      super(delegate);
    }

    @Override
    protected Map<AccessibleObject, String[]> makeMethodCache() {
      return new ConcurrentHashMap<AccessibleObject, String[]>();
    }
  }

}
