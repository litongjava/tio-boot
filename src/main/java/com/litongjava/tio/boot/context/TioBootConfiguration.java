package com.litongjava.tio.boot.context;

import java.io.IOException;

public interface TioBootConfiguration {
  public void config() throws IOException;

  //public <T extends DynamicReloadConfig> Class<T> getDynamicClass();
}
