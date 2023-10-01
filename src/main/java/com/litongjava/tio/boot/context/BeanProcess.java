package com.litongjava.tio.boot.context;

import java.lang.reflect.Method;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.tio.boot.annotation.Bean;

public class BeanProcess {
  public Object process(Class<?> clazz, Method method) {
    try {
      // 调用 @Bean 方法
      Object bean = method.invoke(clazz.getDeclaredConstructor().newInstance());

      // 如果 @Bean 注解中定义了 initMethod，调用该方法进行初始化
      Bean beanAnnotation = method.getAnnotation(Bean.class);
      if (!beanAnnotation.initMethod().isEmpty()) {
        Method initMethod = bean.getClass().getMethod(beanAnnotation.initMethod());
        initMethod.invoke(bean);
      }

      Class<?> returnType = method.getReturnType();
      // TODO: 将bean添加到容器中，或进行其他操作
      // 例如：beanContainer.register(bean);
      //
//      Aop.register(returnType,bean);
      // 为单例注入依赖以后，再添加为单例供后续使用
      AopManager.me().addSingletonObject(returnType, bean);
      Aop.inject(bean);

      return bean;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
