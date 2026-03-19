package com.litongjava.tio.utils.mcid;

import org.junit.Test;

public class McIdUtilsTest {

  @Test
  public void test() {
    for(int i=0;i<1000000;i++) {
      long id = McIdUtils.id();
      System.out.println(id);
    }
  }
}
