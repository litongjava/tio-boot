package com.litongjava.tio.boot.context;

import java.util.List;

public interface Context {
  public void initAnnotation(List<Class<?>> scannedClasses);
}
