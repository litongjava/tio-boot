package com.litongjava.tio.boot.http.controller;

import java.lang.reflect.Method;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public interface ControllerInterceptor {

  public HttpResponse before(HttpRequest request, Method actionMethod);

  public Object after(HttpRequest request, Object targetController, Method actionMethod, Object actionReturnValue);

}
