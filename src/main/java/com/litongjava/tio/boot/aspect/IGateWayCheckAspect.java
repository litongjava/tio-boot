package com.litongjava.tio.boot.aspect;

import java.lang.reflect.Method;

import com.litongjava.aop.GatewayCheck;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public interface IGateWayCheckAspect {
  HttpResponse check(HttpRequest request, Object targetController, Method actionMethod, GatewayCheck annotation);
}
