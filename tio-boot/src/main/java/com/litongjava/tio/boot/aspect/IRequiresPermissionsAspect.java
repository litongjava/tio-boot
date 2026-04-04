package com.litongjava.tio.boot.aspect;

import java.lang.reflect.Method;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

import nexus.io.annotation.RequiresPermissions;

public interface IRequiresPermissionsAspect {
  HttpResponse check(HttpRequest request, Object targetController, Method actionMethod, RequiresPermissions annotation);
}
