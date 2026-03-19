package com.litongjava.tio.utils.cache;

public class RemovalListenerWrapper<T> {
  private T listener;

  public T getListener() {
    return listener;
  }

  public void setListener(T listener) {
    this.listener = listener;
  }

  public RemovalListenerWrapper() {
    super();
  }

  public RemovalListenerWrapper(T listener) {
    super();
    this.listener = listener;
  }

}
