package com.litongjava.tio.boot.context;

@FunctionalInterface
public interface StartupCallback {
  void beforeStart(Class<?>[] primarySources, String[] args);
}
