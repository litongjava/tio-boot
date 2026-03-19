package com.litongjava.tio.boot.spring;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Customization for TioBoot-specific features.
 *
 * @author Brian Clozel
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
public class TioBootWebServerFactoryCustomizer
    implements WebServerFactoryCustomizer<TioBootReactiveWebServerFactory>, Ordered {

  @SuppressWarnings("unused")
  private final Environment environment;

  public TioBootWebServerFactoryCustomizer(Environment environment) {
    this.environment = environment;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(TioBootReactiveWebServerFactory factory) {
    PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();

    customizeRequestDecoder(factory, propertyMapper);
  }

  private void customizeRequestDecoder(TioBootReactiveWebServerFactory factory, PropertyMapper propertyMapper) {

  }

}
