package com.litongjava.tio.boot.http.handler;

import java.lang.reflect.Method;
import java.util.Map;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoute;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.annotation.EnableCORS;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.util.HttpServerResponseUtils;

public class DynamicRequestHandler {

  private RequestActionDispatcher dispatcher = new RequestActionDispatcher();

  public HttpResponse processDynamic(HttpConfig httpConfig, TioBootHttpControllerRoute routes,
      boolean compatibilityAssignment, Map<Class<?>, MethodAccess> classMethodaccessMap, HttpRequest request,
      Method actionMethod) {
    // execute
    HttpResponse response = dispatcher.executeAction(httpConfig, routes, compatibilityAssignment,
        classMethodaccessMap, request, actionMethod);

    boolean isEnableCORS = false;
    EnableCORS enableCORS = actionMethod.getAnnotation(EnableCORS.class);
    if (enableCORS != null) {
      isEnableCORS = true;
      HttpServerResponseUtils.enableCORS(response, new HttpCors(enableCORS));
    }

    if (isEnableCORS == false) {
      Object controllerBean = routes.METHOD_BEAN_MAP.get(actionMethod);
      EnableCORS controllerEnableCORS = controllerBean.getClass().getAnnotation(EnableCORS.class);

      if (controllerEnableCORS != null) {
        isEnableCORS = true;
        HttpServerResponseUtils.enableCORS(response, new HttpCors(controllerEnableCORS));
      }
    }
    return response;
  }

}
