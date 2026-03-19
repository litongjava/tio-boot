package com.litongjava.tio.boot.logging;

import java.lang.reflect.Method;

import com.litongjava.tio.http.common.HttpRequest;

public class LoggingInterceptor {

  /**
   * 在 ControllerInterceptor.before 执行后触发
   *
   * @param request         当前 HttpRequest 对象
   * @param targetController 控制器实例
   * @param actionMethod    即将执行的控制器方法
   * @param paramValues     方法调用时的参数数组
   */
  public void before(HttpRequest request, Object targetController, Method actionMethod, Object[] paramValues) {

  }


  /**
   * 在 ControllerInterceptor.after 执行后触发
   *
   * @param request            当前 HttpRequest 对象
   * @param targetController    控制器实例
   * @param actionMethod       已执行的控制器方法
   * @param paramValues        方法调用时的参数数组
   * @param actionReturnValue  方法返回值
   * @return 继续向下传递的返回结果，一般不做修改
   */
  public Object after(HttpRequest request, Object targetController, Method actionMethod, Object[] paramValues, Object actionReturnValue) {
    return actionReturnValue;
  }
}
