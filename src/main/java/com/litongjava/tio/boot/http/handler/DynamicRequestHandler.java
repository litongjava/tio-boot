package com.litongjava.tio.boot.http.handler;

import java.lang.reflect.Method;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.annotation.EnableCORS;
import com.litongjava.tio.boot.http.router.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.http.utils.RequestActionUtils;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.util.CORSUtils;

public class DynamicRequestHandler {

  public HttpResponse processDynamic(HttpRequest request, HttpConfig httpConfig,
      //
      boolean compatibilityAssignment, TioBootHttpControllerRouter routes, Method actionMethod) {

    // execute
    Object actionReturnValue = executeAction(request, httpConfig, compatibilityAssignment, routes, actionMethod);
    // http response
    HttpResponse response = RequestActionUtils.afterExecuteAction(actionReturnValue);
    EnableCORS enableCORS = actionMethod.getAnnotation(EnableCORS.class);
    if (enableCORS == null) {
      Object controllerBean = routes.METHOD_BEAN_MAP.get(actionMethod);
      enableCORS = controllerBean.getClass().getAnnotation(EnableCORS.class);
    }
    if (enableCORS != null) {
      CORSUtils.enableCORS(response, new HttpCors(enableCORS));
    }
    return response;
  }

  public Object executeAction(HttpRequest request, HttpConfig httpConfig,
      //
      boolean compatibilityAssignment,
      //
      TioBootHttpControllerRouter routes, Method actionMethod) {

    // get paramnames
    String[] paraNames = routes.METHOD_PARAM_NAME_MAP.get(actionMethod);
    // get parameterTypes
    Class<?>[] parameterTypes = routes.METHOD_PARAM_TYPE_MAP.get(actionMethod);// method.getParameterTypes();
    Object controllerBean = routes.METHOD_BEAN_MAP.get(actionMethod);
    Object actionRetrunValue = null;
    if (parameterTypes == null || parameterTypes.length == 0) { // 无请求参数
      // action中没有参数
      MethodAccess methodAccess = TioBootHttpControllerRouter.BEAN_METHODACCESS_MAP.get(controllerBean);
      actionRetrunValue = methodAccess.invoke(controllerBean, actionMethod.getName(), parameterTypes, (Object) null);
    } else {
      // action中有参数
      Object[] paramValues = RequestActionUtils.buildFunctionParamValues(request, httpConfig, compatibilityAssignment, paraNames, parameterTypes);
      MethodAccess methodAccess = TioBootHttpControllerRouter.BEAN_METHODACCESS_MAP.get(controllerBean);
      actionRetrunValue = methodAccess.invoke(controllerBean, actionMethod.getName(), parameterTypes, paramValues);
    }

    return actionRetrunValue;
  }

}
