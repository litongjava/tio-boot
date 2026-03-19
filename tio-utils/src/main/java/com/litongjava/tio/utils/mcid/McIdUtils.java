package com.litongjava.tio.utils.mcid;

import com.litongjava.tio.utils.hutool.RandomUtils;

public class McIdUtils {

  // 单例的 SnowflakeIdGenerator 实例
  private static final int mid = RandomUtils.nextInt(1, 255);
  private static final McIdGenerator generator = new McIdGenerator(mid);

  public static long id() {
    return generator.generateId();
  }
  
  public static int getMid() {
    return mid;
  }
}
