package com.litongjava.tio.boot.spring;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.litongjava.tio.boot.server.TioBootServer;

@Configuration()
@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
@ConditionalOnClass({ TioBootServer.class })
public class EmbeddedTioBoot {

  @Bean
  public TioBootReactiveWebServerFactory tioBootReactiveWebServerFactory(
      ObjectProvider<TioBootRouteProvider> routes, ObjectProvider<TioBootServerCustomizer> serverCustomizers) {

    TioBootReactiveWebServerFactory serverFactory = new TioBootReactiveWebServerFactory();
    //serverFactory.setResourceFactory(resourceFactory);
    routes.orderedStream().forEach(serverFactory::addRouteProviders);

    List<TioBootServerCustomizer> collect = serverCustomizers.orderedStream().collect(Collectors.toList());
    serverFactory.getServerCustomizers().addAll(collect);
    return serverFactory;
  }
}