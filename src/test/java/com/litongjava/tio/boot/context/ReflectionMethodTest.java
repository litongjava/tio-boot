package com.litongjava.tio.boot.context;

import java.lang.reflect.Method;
import java.util.Map;

public class ReflectionMethodTest {

  public void index() {

  }

  public Map<String, Object> login() {
    return null;
  }

  public static void main(String[] args) {
    Method[] declaredMethods = ReflectionMethodTest.class.getDeclaredMethods();
    for (Method method : declaredMethods) {
      if (method.getReturnType()==Void.TYPE) {
        System.out.println(method.getReturnType());
      }

    }
  }
}
