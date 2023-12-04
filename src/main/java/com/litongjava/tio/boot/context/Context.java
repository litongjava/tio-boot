package com.litongjava.tio.boot.context;

import java.util.List;

import org.tio.server.TioServer;

public interface Context {
  public void initAnnotation(List<Class<?>> scannedClasses);

  public Context run(Class<?>[] primarySources, String[] args);

  public boolean isRunning();

  public void close();

  public void restart(Class<?>[] primarySources, String[] args);

  public TioServer getServer();

}
