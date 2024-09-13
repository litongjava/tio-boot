package com.litongjava.tio.boot.utils;

import java.lang.reflect.Method;

import com.litongjava.tio.http.server.handler.IHttpRequestFunction;

public class ApplyFunctionUtils {

  public static Method getMethod(Class<? extends IHttpRequestFunction> clazz) {
    // 通过反射获取函数的参数类型
    Method method = null;
    try {
      method = clazz.getDeclaredMethod("handle", Object.class);
    } catch (NoSuchMethodException | SecurityException e) {
      e.printStackTrace();
      return null;
    }
    return method;
  }
}
