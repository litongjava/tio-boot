package nexus.io.tio.boot.http.handler.internal;

import com.litongjava.jfinal.aop.Aop;

import nexus.io.controller.ControllerFactory;

public class AopControllerFactory implements ControllerFactory {

  @Override
  public Object getInstance(Class<?> controllerClazz) throws Exception {
    return Aop.get(controllerClazz);
  }
}
