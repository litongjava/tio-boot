package com.litongjava.tio.boot.context;

@FunctionalInterface
public interface StartedCallBack {
  void afterStarted(Class<?>[] primarySources, String[] args);
}
