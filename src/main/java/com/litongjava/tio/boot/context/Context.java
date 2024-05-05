package com.litongjava.tio.boot.context;

import java.util.List;

import com.litongjava.tio.server.TioServer;

public interface Context {

  public Context run(Class<?>[] primarySources, String[] args);

  public Context run(Class<?>[] primarySources, TioBootConfiguration tioBootConfiguration, String[] args);

  public void initAnnotation(List<Class<?>> scannedClasses);

  public boolean isRunning();

  public void close();

  public void restart(Class<?>[] primarySources, String[] args);

  public TioServer getServer();

}
