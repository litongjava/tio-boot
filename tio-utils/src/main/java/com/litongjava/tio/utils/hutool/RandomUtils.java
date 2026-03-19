package com.litongjava.tio.utils.hutool;

import java.util.Random;

public class RandomUtils {

  public static int nextInt(int min, int max) {
    Random random = new Random();
    // nextInt(max - min + 1) 生成0到(max - min)之间的随机数，然后加上min以便生成1到max之间的随机数
    int randomNumber = random.nextInt(max - min + 1) + min;
    return randomNumber;
  }
}
