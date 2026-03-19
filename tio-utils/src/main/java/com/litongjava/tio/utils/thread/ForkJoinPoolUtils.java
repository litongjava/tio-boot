package com.litongjava.tio.utils.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.CompletableFuture;

public class ForkJoinPoolUtils {
  public static volatile ForkJoinPool customThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 4);

  public CompletableFuture<Void> runAsync(Runnable runnable) {
    return CompletableFuture.runAsync(runnable, customThreadPool);
  }

  public void shutdown() {
    customThreadPool.shutdown();
  }
}
