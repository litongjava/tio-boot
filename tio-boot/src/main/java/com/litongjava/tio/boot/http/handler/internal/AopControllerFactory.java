package com.litongjava.tio.boot.http.handler.internal;

import com.litongjava.controller.ControllerFactory;
import com.litongjava.jfinal.aop.Aop;

public class AopControllerFactory implements ControllerFactory {

  @Override
  public Object getInstance(Class<?> controllerClazz) throws Exception {
    return Aop.get(controllerClazz);
  }
}
