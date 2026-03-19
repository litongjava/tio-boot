package com.litongjava.tio.boot.spring;

import org.springframework.context.annotation.Import;

@Import({ EmbeddedTioBoot.class, TioBootWebServerFactoryCustomizer.class })
public class TioBootServerAutoConfiguration {
}
