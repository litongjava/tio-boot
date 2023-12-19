package com.litongjava.tio.boot.context;

import java.util.List;

import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.server.TioServer;

public interface Context {

  public Context run(Class<?>[] primarySources, String[] args);

  public Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart);

  public Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted);

  public Context run(Class<?>[] primarySources, String[] args, StartupCallback beforeStart,
      StartedCallBack afterStarted, ShutdownCallback beforeStop, ShutCallback afterStoped);

  public Context run(Class<?>[] primarySources, String[] args, ShutdownCallback beforeStop);

  public Context run(Class<?>[] primarySources, String[] args, ShutdownCallback beforeStop, ShutCallback afterStoped);

  public void initAnnotation(List<Class<?>> scannedClasses);

  public boolean isRunning();

  public void close();

  public void restart(Class<?>[] primarySources, String[] args);

  public TioServer getServer();

  public TioBootServer getTioBootServer();

  public StartupCallback getBeforeStart();

  public StartedCallBack getAfterStarted();

  public ShutdownCallback getBeforeStop();

  public ShutCallback getafterStoped();

}
