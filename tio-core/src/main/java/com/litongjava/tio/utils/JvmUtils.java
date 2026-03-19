package com.litongjava.tio.utils;

public class JvmUtils {

  // 检查是否在标准Java环境中运行
  public static boolean isStandardJava() {
    try {
      // 尝试加载一个仅在标准Java中存在的类
      Class.forName("java.lang.management.ManagementFactory");
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
