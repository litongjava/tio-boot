package com.litongjava.tio.boot.http.handler;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.http.server.mvc.intf.ControllerFactory;

public class JFinalAopControllerFactory implements ControllerFactory {

  @Override
  public Object getInstance(Class<?> controllerClazz) throws Exception {
    return Aop.get(controllerClazz);
  }
}
