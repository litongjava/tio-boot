package com.litongjava.tio.boot.http.handler.controller;

import java.lang.reflect.Method;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.annotation.EnableCORS;
import com.litongjava.annotation.GatewayCheck;
import com.litongjava.annotation.RequiresAuthentication;
import com.litongjava.annotation.RequiresPermissions;
import com.litongjava.tio.boot.aspect.IGateWayCheckAspect;
import com.litongjava.tio.boot.aspect.IRequiresAuthenticationAspect;
import com.litongjava.tio.boot.aspect.IRequiresPermissionsAspect;
import com.litongjava.tio.boot.http.utils.RequestActionUtils;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.util.CORSUtils;

public class DynamicRequestController {

  private IGateWayCheckAspect gateWayCheckAspect = null;
  private IRequiresAuthenticationAspect requiresAuthenticationAspect = null;
  private IRequiresPermissionsAspect requiresPermissionsAspect = null;

  public HttpResponse process(HttpRequest request, HttpConfig httpConfig,
      //
      boolean compatibilityAssignment, TioBootHttpControllerRouter routes, Method actionMethod) {

    // execute
    HttpResponse response = null;
    Object targetController = routes.METHOD_BEAN_MAP.get(actionMethod);
    response = beforeAction(request, targetController, actionMethod);
    if (response != null) {
      return response;
    }
    Object actionReturnValue = executeAction(request, httpConfig, compatibilityAssignment, routes, targetController, actionMethod);
    response = afterAction(targetController, actionMethod, actionReturnValue);
    return response;
  }

  private HttpResponse beforeAction(HttpRequest request, Object targetController, Method actionMethod) {

    // GatewayCheck
    if (actionMethod.isAnnotationPresent(GatewayCheck.class)) {
      GatewayCheck gatewayCheckAnnotation = actionMethod.getAnnotation(GatewayCheck.class);
      if (gatewayCheckAnnotation != null) {
        if (gateWayCheckAspect == null) {
          gateWayCheckAspect = TioBootServer.me().getGateWayCheckAspect();
        }
        if (gateWayCheckAspect != null) {
          return gateWayCheckAspect.check(request, targetController, actionMethod, gatewayCheckAnnotation);
        }
      }
    }

    // RequiresAuthentication
    if (actionMethod.isAnnotationPresent(RequiresAuthentication.class)) {
      if (requiresAuthenticationAspect == null) {
        requiresAuthenticationAspect = TioBootServer.me().getRequiresAuthenticationAspect();
      }
      if (requiresAuthenticationAspect != null) {
        return requiresAuthenticationAspect.check(request, targetController, actionMethod);
      }
    }

    // RequiresPermissions
    if (actionMethod.isAnnotationPresent(RequiresPermissions.class)) {
      RequiresPermissions requiresPermissionsAnnotation = actionMethod.getAnnotation(RequiresPermissions.class);
      if (requiresPermissionsAnnotation != null) {
        //IRequiresPermissionsAspect
        if (requiresPermissionsAspect == null) {
          requiresPermissionsAspect = TioBootServer.me().getRequiresPermissionsAspect();
        }

        if (requiresPermissionsAspect != null) {
          return requiresPermissionsAspect.check(request, targetController, actionMethod, requiresPermissionsAnnotation);
        }
      }
    }

    return null; // continue action
  }

  private HttpResponse afterAction(Object targetController, Method actionMethod, Object actionReturnValue) {
    // http response
    HttpResponse response = RequestActionUtils.afterExecuteAction(actionReturnValue);
    EnableCORS enableCORS = actionMethod.getAnnotation(EnableCORS.class);
    if (enableCORS == null) {
      enableCORS = targetController.getClass().getAnnotation(EnableCORS.class);
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
      TioBootHttpControllerRouter routes, Object targetController, Method actionMethod) {

    // get paramnames
    String[] paraNames = routes.METHOD_PARAM_NAME_MAP.get(actionMethod);
    // get parameterTypes
    Class<?>[] parameterTypes = routes.METHOD_PARAM_TYPE_MAP.get(actionMethod);// method.getParameterTypes();

    Object actionRetrunValue = null;
    if (parameterTypes == null || parameterTypes.length == 0) { // 无请求参数
      // action中没有参数
      MethodAccess methodAccess = TioBootHttpControllerRouter.BEAN_METHODACCESS_MAP.get(targetController);
      actionRetrunValue = methodAccess.invoke(targetController, actionMethod.getName(), parameterTypes, (Object) null);
    } else {
      // action中有参数
      Object[] paramValues = RequestActionUtils.buildFunctionParamValues(request, httpConfig, compatibilityAssignment,
          //
          paraNames, parameterTypes, actionMethod.getGenericParameterTypes());
      MethodAccess methodAccess = TioBootHttpControllerRouter.BEAN_METHODACCESS_MAP.get(targetController);
      actionRetrunValue = methodAccess.invoke(targetController, actionMethod.getName(), parameterTypes, paramValues);
    }

    return actionRetrunValue;
  }

}
