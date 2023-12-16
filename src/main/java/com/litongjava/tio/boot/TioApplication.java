package com.litongjava.tio.boot;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.context.Context;
import com.litongjava.tio.boot.context.ShutCallback;
import com.litongjava.tio.boot.context.ShutdownCallback;
import com.litongjava.tio.boot.context.StartedCallBack;
import com.litongjava.tio.boot.context.StartupCallback;
import com.litongjava.tio.boot.context.TioApplicationContext;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;

/**
 * @author Ping E Lee
 */
public class TioApplication {

  public static Context run(Class<?> primarySource, String... args) {
    return run(new Class<?>[] { primarySource }, args);
  }

  public static Context run(Class<?> primarySource, ServerAioHandler demoHandler, String... args) {
    return run(new Class<?>[] { primarySource }, demoHandler, args);
  }

  public static Context run(Class<?> primarySource, ServerAioHandler handler, ServerAioListener listener,
      String[] args) {
    return run(new Class<?>[] { primarySource }, handler, listener, args);
  }

  public static Context run(Class<?> primarySource, ServerAioListener listener, String[] args) {
    return run(new Class<?>[] { primarySource }, listener, args);

  }

  private static Context run(Class<?>[] primarySources, ServerAioListener listener, String[] args) {
    return run(primarySources, null, listener, args);
  }

  public static Context run(Class<?>[] primarySources, String[] args) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, null, null, args);
  }

  public static Context run(Class<?>[] primarySources, ServerAioHandler tcpHandler, String[] args) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, tcpHandler, args);
  }

  public static Context run(Class<?>[] primarySources, ServerAioHandler tcpHandler, ServerAioListener listener,
      String[] args) {
    Context context = Aop.get(TioApplicationContext.class);
    return context.run(primarySources, tcpHandler, listener, args);
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