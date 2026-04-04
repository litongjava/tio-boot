package nexus.io.tio.boot.http.controller;

import java.lang.reflect.Method;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

public interface ControllerInterceptor {

  public HttpResponse before(HttpRequest request, Method actionMethod);

  public Object after(HttpRequest request, Object targetController, Method actionMethod, Object actionReturnValue);

}
