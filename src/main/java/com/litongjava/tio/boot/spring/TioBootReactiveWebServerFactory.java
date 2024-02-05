package com.litongjava.tio.boot.spring;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

import com.litongjava.tio.boot.server.TioBootServer;

public class TioBootReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

  private Set<TioBootServerCustomizer> serverCustomizers = new LinkedHashSet<>();

  private List<TioBootRouteProvider> routeProviders = new ArrayList<>();

  private Duration lifecycleTimeout;

  private boolean useForwardHeaders;

  private ReactorResourceFactory resourceFactory;

  public TioBootReactiveWebServerFactory() {
  }

  public TioBootReactiveWebServerFactory(int port) {
    super(port);
  }

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    TioBootServer tioBootServer = createTioBootServer();
    ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
    TioBootWebServer webServer = createTioBootWebServer(tioBootServer, handlerAdapter, this.lifecycleTimeout);
    webServer.setRouteProviders(this.routeProviders);
    return webServer;
  }

  TioBootWebServer createTioBootWebServer(TioBootServer tioBootServer, ReactorHttpHandlerAdapter handlerAdapter,
      Duration lifecycleTimeout) {
    return new TioBootWebServer(tioBootServer, handlerAdapter, lifecycleTimeout);
  }

  /**
   * Returns a mutable collection of the {@link TioBootServerCustomizer}s that will be
   * applied to the TioBoot server builder.
   * @return the customizers that will be applied
   */
  public Collection<TioBootServerCustomizer> getServerCustomizers() {
    return this.serverCustomizers;
  }

  /**
   * Set {@link TioBootServerCustomizer}s that should be applied to the TioBoot server
   * builder. Calling this method will replace any existing customizers.
   * @param serverCustomizers the customizers to set
   */
  public void setServerCustomizers(Collection<? extends TioBootServerCustomizer> serverCustomizers) {
    Assert.notNull(serverCustomizers, "ServerCustomizers must not be null");
    this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
  }

  /**
   * Add {@link TioBootServerCustomizer}s that should applied while building the server.
   * @param serverCustomizers the customizers to add
   */
  public void addServerCustomizers(TioBootServerCustomizer... serverCustomizers) {
    Assert.notNull(serverCustomizers, "ServerCustomizer must not be null");
    this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
  }

  /**
   * Add {@link TioBootRouteProvider}s that should be applied, in order, before the
   * handler for the Spring application.
   * @param routeProviders the route providers to add
   */
  public void addRouteProviders(TioBootRouteProvider... routeProviders) {
    Assert.notNull(routeProviders, "TioBootRouteProvider must not be null");
    this.routeProviders.addAll(Arrays.asList(routeProviders));
  }

  /**
   * Set the maximum amount of time that should be waited when starting or stopping the
   * server.
   * @param lifecycleTimeout the lifecycle timeout
   */
  public void setLifecycleTimeout(Duration lifecycleTimeout) {
    this.lifecycleTimeout = lifecycleTimeout;
  }

  /**
   * Set if x-forward-* headers should be processed.
   * @param useForwardHeaders if x-forward headers should be used
   * @since 2.1.0
   */
  public void setUseForwardHeaders(boolean useForwardHeaders) {
    this.useForwardHeaders = useForwardHeaders;
  }

  /**
   * Set the {@link ReactorResourceFactory} to get the shared resources from.
   * @param resourceFactory the server resources
   * @since 2.1.0
   */
  public void setResourceFactory(ReactorResourceFactory resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  private TioBootServer createTioBootServer() {
    TioBootServer server = TioBootServer.create();
    return applyCustomizers(server);
  }

  private InetSocketAddress getListenAddress() {
    if (getAddress() != null) {
      return new InetSocketAddress(getAddress().getHostAddress(), getPort());
    }
    return new InetSocketAddress(getPort());
  }

  private TioBootServer applyCustomizers(TioBootServer server) {
    for (TioBootServerCustomizer customizer : this.serverCustomizers) {
      server = customizer.apply(server);
    }
    return server;
  }

}
