package com.litongjava.tio.boot.aspect;

import java.lang.reflect.Method;

import nexus.io.annotation.RequiresPermissions;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

public interface IRequiresPermissionsAspect {
  HttpResponse check(HttpRequest request, Object targetController, Method actionMethod, RequiresPermissions annotation);
}
