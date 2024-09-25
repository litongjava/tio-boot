package com.litongjava.tio.boot.spring;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

import com.litongjava.context.Context;
import com.litongjava.tio.boot.TioApplication;
import com.litongjava.tio.boot.server.TioBootServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioBootWebServer implements WebServer {

  /**
   * Permission denied error code from {@code errno.h}.
   */
  @SuppressWarnings("unused")
  private static final int ERROR_NO_EACCES = -13;

  @SuppressWarnings("unused")
  private final TioBootServer tioBootServer;

  @SuppressWarnings("unused")
  private final Duration lifecycleTimeout;

  private List<TioBootRouteProvider> routeProviders = Collections.emptyList();

  @SuppressWarnings("unused")
  private ReactorHttpHandlerAdapter handler;

  private Context context;

  public TioBootWebServer(TioBootServer tioBootServer, ReactorHttpHandlerAdapter handlerAdapter, Duration lifecycleTimeout) {
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
    for (TioBootRouteProvider provider : routeProviders) {
      log.info("{}", provider);
    }
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
    return context.getPort();
  }
}
