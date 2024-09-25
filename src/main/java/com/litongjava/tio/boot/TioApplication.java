package com.litongjava.tio.boot;

import com.litongjava.context.BootConfiguration;
import com.litongjava.context.Context;
import com.litongjava.tio.boot.context.TioApplicationContext;

/**
 * @author Ping E Lee
 */
public class TioApplication {

  public static Context run(Class<?> primarySource, String[] args) {
    return run(new Class<?>[] { primarySource }, args);
  }

  public static Context run(Class<?>[] primarySources, String[] args) {
    Context context = new TioApplicationContext();
    return context.run(primarySources, args);
  }

  public static Context run(Class<?> primarySource, BootConfiguration tioBootConfiguration, String[] args) {
    return run(new Class<?>[] { primarySource }, tioBootConfiguration, args);
  }

  public static Context run(Class<?>[] primarySources, BootConfiguration tioBootConfiguration, String[] args) {
    // Context context = new ;
    // Context context = Aop.get(TioApplicationContext.class);
    Context context = new TioApplicationContext();
    return context.run(primarySources, tioBootConfiguration, args);
  }

}