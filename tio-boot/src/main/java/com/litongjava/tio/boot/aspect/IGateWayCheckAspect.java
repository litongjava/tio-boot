package com.litongjava.tio.boot.aspect;

import java.lang.reflect.Method;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

import nexus.io.annotation.GatewayCheck;

public interface IGateWayCheckAspect {
  HttpResponse check(HttpRequest request, Object targetController, Method actionMethod, GatewayCheck annotation);
}
