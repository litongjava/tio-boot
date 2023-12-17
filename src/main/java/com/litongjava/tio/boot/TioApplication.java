package com.litongjava.tio.boot;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.context.Context;
import com.litongjava.tio.boot.context.ShutCallback;
import com.litongjava.tio.boot.context.ShutdownCallback;
import com.litongjava.tio.boot.context.StartedCallBack;
import com.litongjava.tio.boot.context.StartupCallback;
import com.litongjava.tio.boot.context.TioApplicationContext;

/**
 * @author Ping E Lee
 */
public class TioApplication {

  public static Context run(Class<?> primarySource, String[] args, StartupCallback beforeStart) {
    return run(new Class<?>[] { primarySource }, args, beforeStart);
  }

  public static Context run(Class<?> primarySource, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted, ShutdownCallback beforeStop, ShutCallback afterStoped) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(new Class<?>[] { primarySource }, args, beforeStart, afterStarted, beforeStop, afterStoped);
  }

  public static Context run(Class<?>[] primarySources, String[] args) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args);
  }

  public static Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args, beforeStart);
  }

  public static Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args, beforeStart, afterStarted);
  }

  public static Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted, ShutdownCallback beforeStop, ShutCallback afterStoped) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args, beforeStart, afterStarted, beforeStop, afterStoped);
  }

  public static Context run(Class<?>[] primarySources, String[] args, ShutdownCallback beforeStop) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args, beforeStop);

  }

  public static Context run(Class<?>[] primarySources, String[] args, ShutdownCallback beforeStop,
      ShutCallback afterStoped) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, args, beforeStop, afterStoped);
  }

}