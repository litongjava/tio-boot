package com.litongjava.tio.boot.spring;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

import com.litongjava.tio.boot.TioApplication;
import com.litongjava.tio.boot.context.Context;
import com.litongjava.tio.boot.server.TioBootServer;

public class TioBootWebServer implements WebServer {

  /**
   * Permission denied error code from {@code errno.h}.
   */
  private static final int ERROR_NO_EACCES = -13;

  private static final Log logger = LogFactory.getLog(TioBootWebServer.class);

  private final TioBootServer tioBootServer;

  private final Duration lifecycleTimeout;

  private List<TioBootRouteProvider> routeProviders = Collections.emptyList();

  private ReactorHttpHandlerAdapter handler;

  private Context context;

  public TioBootWebServer(TioBootServer tioBootServer, ReactorHttpHandlerAdapter handlerAdapter,
      Duration lifecycleTimeout, org.springframework.boot.web.server.Shutdown shutdown) {
    Assert.notNull(tioBootServer, "tioBootServer must not be null");
    Assert.notNull(handlerAdapter, "HandlerAdapter must not be null");
    this.tioBootServer = tioBootServer;
    this.lifecycleTimeout = lifecycleTimeout;
    this.handler = handlerAdapter;
  }

  public void setRouteProviders(List<TioBootRouteProvider> routeProviders) {
    this.routeProviders = routeProviders;
  }

  @Override
  public void start() throws WebServerException {
    // 启动服务
    Class<?> primarySource = SpringBootArgs.getPrimarySource();
    String[] args = SpringBootArgs.getArgs();
    context = TioApplication.run(primarySource, args);
  }

  @Override
  public void stop() throws WebServerException {
    try {
      context.close();
    } catch (Exception e) {
    }

  }

  @Override
  public int getPort() {
    return context.getServer().getServerNode().getPort();
  }
}
