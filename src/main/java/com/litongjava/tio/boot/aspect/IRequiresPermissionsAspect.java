package com.litongjava.tio.boot.aspect;

import java.lang.reflect.Method;

import com.litongjava.aop.RequiresPermissions;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public interface IRequiresPermissionsAspect {
  HttpResponse check(HttpRequest request, Object targetController, Method actionMethod, RequiresPermissions annotation);
}
