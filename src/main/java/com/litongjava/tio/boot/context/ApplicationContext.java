package com.litongjava.tio.boot.context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jfinal.aop.AopManager;
import com.litongjava.jfinal.aop.Autowired;
import com.litongjava.tio.boot.annotation.Bean;
import com.litongjava.tio.boot.annotation.Component;
import com.litongjava.tio.boot.annotation.Configuration;
import com.litongjava.tio.boot.annotation.Controller;
import com.litongjava.tio.boot.annotation.Repository;
import com.litongjava.tio.boot.annotation.Service;

public class ApplicationContext implements Context {

  // 创建一个队列来存储 process 方法的返回值
  private Queue<Object> beans = new LinkedList<>();

  @SuppressWarnings("unchecked")
  public void initAnnotation(List<Class<?>> scannedClasses) {
    if (scannedClasses == null) {
      return;
    }
    BeanProcess beanProcess = new BeanProcess();
    // 1. 显式地先初始化Bean
    for (Class<?> clazz : scannedClasses) {
      if (isComponent(clazz)) {
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
          AopManager.me().addMapping((Class<Object>) interfaces[0], (Class<? extends Object>) clazz);
        }
        Object object = Aop.get(clazz);
        beans.add(object);
        for (Method method : clazz.getDeclaredMethods()) {
          if (method.isAnnotationPresent(Bean.class)) {
            beans.add(beanProcess.process(clazz, method));
          }
        }
      }
    }

    // 处理autoWird注解
    processAutowired();

  }

  private void processAutowired() {
    for (Object bean : beans) {
      Class<?> clazz = bean.getClass();
      for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Autowired.class)) {
          Object value = Aop.get(field.getType());
          try {
            field.setAccessible(true);
            field.set(bean, value);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private boolean isComponent(Class<?> clazz) {
    return clazz.isAnnotationPresent(Component.class) || clazz.isAnnotationPresent(Controller.class)
        || clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Repository.class)
        || clazz.isAnnotationPresent(Configuration.class);
  }

}
