package com.litongjava.tio.boot;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.context.Context;
import com.litongjava.tio.boot.context.TioApplicationContext;

/**
 * @author Ping E Lee
 */
public class TioApplication {

  public static Context run(Class<?> primarySource, String... args) {
    return run(new Class<?>[] { primarySource }, args);
  }

  public static Context run(Class<?>[] primarySources, String[] args) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args);

  }
}