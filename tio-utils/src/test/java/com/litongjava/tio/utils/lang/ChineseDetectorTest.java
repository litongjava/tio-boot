package com.litongjava.tio.utils.lang;

import org.junit.Test;

public class ChineseDetectorTest {

  @Test
  public void test() {
    String text = "现在我们学习如何对函数的四则运算求导。加减法则：[f(x)±g(x)]' = f'(x)±g'(x)。乘法法则：[f(x)·g(x)]' = f'(x)g(x) + f(x)g'(x)，注意不是两项导数的乘积。除法法则：[f(x)/g(x)]' = [f'(x)g(x) - f(x)g'(x)]/[g(x)]²，g(x)≠0。例如：(x²sin x)' = 2x·sin x + x²·cos x。";
    boolean isChinese = ChineseDetector.isChinese(text);
    System.out.println(isChinese);
  }
}
