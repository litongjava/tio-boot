package nexus.io.tio.boot.http.handler.internal;

import nexus.io.controller.ControllerFactory;
import nexus.io.jfinal.aop.Aop;

public class AopControllerFactory implements ControllerFactory {

  @Override
  public Object getInstance(Class<?> controllerClazz) throws Exception {
    return Aop.get(controllerClazz);
  }
}
