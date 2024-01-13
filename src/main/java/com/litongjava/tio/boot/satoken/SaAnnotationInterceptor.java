package com.litongjava.tio.boot.satoken;

import com.litongjava.jfinal.aop.Interceptor;
import com.litongjava.jfinal.aop.Invocation;

import cn.dev33.satoken.strategy.SaStrategy;

/**
 * 注解式鉴权 - 拦截器
 */
public class SaAnnotationInterceptor implements Interceptor {
  @Override
  public void intercept(Invocation invocation) {
    SaStrategy.instance.checkMethodAnnotation.accept((invocation.getMethod()));
    invocation.invoke();
  }
}
