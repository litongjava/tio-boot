package com.litongjava.tio.utils.cache.mapcache;

import java.io.Serializable;

import org.junit.Test;

public class ConcurrentMapCacheFactoryTest {

  @Test
  public void test() {
    ConcurrentMapCache cache = ConcurrentMapCacheFactory.INSTANCE.register("student", 60L, 60L);
    cache.put("litong", "17");

    ConcurrentMapCache cache2 = ConcurrentMapCacheFactory.INSTANCE.getCache("student");
    Serializable serializable = cache2.get("litong");
    System.out.println(serializable);

  }

  @Test
  public void testRegisterStringLongLong() {
  }

  @Test
  public void testRegisterStringLongLongRemovalListenerWrapperOfT() {
  }

  @Test
  public void testGetCacheStringBoolean() {
  }

  @Test
  public void testGetCacheString() {
  }

}
