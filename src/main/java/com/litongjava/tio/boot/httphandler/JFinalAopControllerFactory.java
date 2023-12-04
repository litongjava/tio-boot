package com.litongjava.tio.boot.httphandler;

import org.tio.http.server.mvc.intf.ControllerFactory;

import com.litongjava.jfinal.aop.Aop;

public class JFinalAopControllerFactory implements ControllerFactory {

  @Override
  public Object getInstance(Class<?> controllerClazz) throws Exception {
    return Aop.get(controllerClazz);
  }
}
