package com.litongjava.tio.server;

import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

public class AcceptCompletionHandlerTest {

  @Test
  public void test() {
    int threadCount = ForkJoinPool.commonPool().getParallelism();
    System.out.println("默认的线程数: " + threadCount);

  }

}
