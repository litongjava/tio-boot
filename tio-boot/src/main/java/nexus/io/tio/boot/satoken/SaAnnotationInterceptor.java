package nexus.io.tio.boot.satoken;

import cn.dev33.satoken.strategy.SaStrategy;
import nexus.io.jfinal.aop.AopInterceptor;
import nexus.io.jfinal.aop.AopInvocation;

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
