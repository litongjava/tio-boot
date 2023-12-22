package com.litongjava.tio.boot.server;

import com.litongjava.tio.boot.context.Context;

public interface TioBootServerListener {

  public void boforeStart(Class<?>[] primarySources, String[] args);

  public void afterStarted(Class<?>[] primarySources, String[] args, Context context);

  public void beforeStop();

  public void afterStoped();
  
}
