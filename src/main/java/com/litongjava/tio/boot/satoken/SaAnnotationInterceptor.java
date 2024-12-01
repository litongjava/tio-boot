package com.litongjava.tio.boot.satoken;

import com.litongjava.jfinal.aop.AopInterceptor;
import com.litongjava.jfinal.aop.AopInvocation;

import cn.dev33.satoken.strategy.SaStrategy;

/**
 * 注解式鉴权 - 拦截器
 */
public class SaAnnotationInterceptor implements AopInterceptor {
  @Override
  public void intercept(AopInvocation invocation) {
    SaStrategy.instance.checkMethodAnnotation.accept((invocation.getMethod()));
    invocation.invoke();
  }
}
