package com.litongjava.tio.boot.context;

import org.junit.Test;

import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.tio.boot.http.handler.DefaultHttpRequestHandler;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;

public class TioApplicationContextTest {

  @Test
  public void test() throws Exception {
    HttpConfig httpConfig = new HttpConfig(null, false);
    HttpRequestHandler handler = null;
    boolean contains = AopManager.me().getAopFactory().contains(HttpRequestHandler.class);
    if (!contains) {
      handler = new DefaultHttpRequestHandler(httpConfig, TioApplicationContextTest.class);
      AopManager.me().addMapping(HttpRequestHandler.class, handler.getClass());
      AopManager.me().addSingletonObject(handler);
    }
    
    contains = AopManager.me().getAopFactory().contains(HttpRequestHandler.class);
    System.out.println(contains);

  }

}
