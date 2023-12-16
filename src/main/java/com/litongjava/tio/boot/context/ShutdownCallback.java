package com.litongjava.tio.boot.context;

@FunctionalInterface
public interface ShutdownCallback {
  void beforeStop();
}