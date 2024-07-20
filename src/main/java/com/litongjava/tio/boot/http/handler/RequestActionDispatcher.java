package com.litongjava.tio.boot.http.handler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.jfinal.template.Template;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.http.routes.TioBootHttpControllerRoute;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.server.util.ClassUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.server.ServerChannelContext;
import com.litongjava.tio.utils.hutool.BeanUtil;
import com.litongjava.tio.utils.hutool.ClassUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.lock.LockUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by litonglinux@qq.com on 11/9/2023_2:29 AM
 */
@Slf4j
public class RequestActionDispatcher {
  /**
   * 获取class中可以访问的方法
   * @param classMethodaccessMap
   * @param clazz
   * @return
   * @throws Exception
   */
  private MethodAccess getMethodAccess(Map<Class<?>, MethodAccess> classMethodaccessMap, Class<?> clazz)
      throws Exception {
    MethodAccess ret = classMethodaccessMap.get(clazz);
    if (ret == null) {
      LockUtils.runWriteOrWaitRead("_tio_http_h_ma_" + clazz.getName(), clazz, () -> {
        // @Override
        // public void read() {
        // }

        // @Override
        // public void write() {
        // MethodAccess ret = CLASS_METHODACCESS_MAP.get(clazz);
        if (classMethodaccessMap.get(clazz) == null) {
          // ret = MethodAccess.get(clazz);
          classMethodaccessMap.put(clazz, MethodAccess.get(clazz));
        }
        // }
      });
      ret = classMethodaccessMap.get(clazz);
    }
    return ret;
  }

  public HttpResponse executeAction(HttpConfig httpConfig, TioBootHttpControllerRoute routes, boolean compatibilityAssignment,
      Map<Class<?>, MethodAccess> classMethodaccessMap, HttpRequest request, Method actionMethod) {
    String method = request.getMethod();
    if ("OPTIONS".equals(method)) {
      // if is OPTIONS method,just return
      return TioRequestContext.getResponse();
    }
    // get paramnames
    String[] paramnames = routes.METHOD_PARAMNAME_MAP.get(actionMethod);
    // get parameterTypes
    Class<?>[] parameterTypes = routes.METHOD_PARAMTYPE_MAP.get(actionMethod);// method.getParameterTypes();
    Object controllerBean = routes.METHOD_BEAN_MAP.get(actionMethod);
    Object actionRetrunValue = null;
    if (parameterTypes == null || parameterTypes.length == 0) { // 无请求参数
      // action中没有参数
      actionRetrunValue = TioBootHttpControllerRoute.BEAN_METHODACCESS_MAP.get(controllerBean).invoke(controllerBean,
          actionMethod.getName(), parameterTypes, (Object) null);
    } else {
      // action中有残杀
      actionRetrunValue = executeActionWithParas(httpConfig, compatibilityAssignment, classMethodaccessMap, request,
          actionMethod, paramnames, parameterTypes, controllerBean);
    }

    return afterExecuteAction(request, actionRetrunValue);
  }

  /**
   * 
   * @param httpConfig
   * @param compatibilityAssignment
   * @param classMethodaccessMap
   * @param request
   * @param actionMethod
   * @param paramnames
   * @param parameterTypes
   * @param controllerBean
   * @return
   */
  private Object executeActionWithParas(HttpConfig httpConfig, boolean compatibilityAssignment,
      Map<Class<?>, MethodAccess> classMethodaccessMap, HttpRequest request, Method actionMethod, String[] paramnames,
      Class<?>[] parameterTypes, Object controllerBean) {
    Object actionRetrunValue;
    // 赋值这段代码待重构，先用上
    Object[] paramValues = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> paramType = parameterTypes[i];
      if (paramType == HttpRequest.class) {
        paramValues[i] = request;
        continue;
      } else if (paramType == HttpResponse.class) {
        paramValues[i] = new HttpResponse(request);
        continue;
      } else {
        if (compatibilityAssignment) {
          if (paramType == HttpSession.class) {
            paramValues[i] = request.getHttpSession();
            continue;
          } else if (paramType == HttpConfig.class) {
            paramValues[i] = httpConfig;
            continue;
          } else if (paramType == ServerChannelContext.class) { // paramType.isAssignableFrom(ServerChannelContext.class)
            paramValues[i] = request.channelContext;
            continue;
          }
        }

        Map<String, Object[]> params = request.getParams();
        if (params != null && params.size() > 0) {
          try {
            injectRequestParameters(classMethodaccessMap, paramnames, paramValues, i, paramType, params);
          } catch (Exception e) {
            log.error("error while inject request parameters:{},{}", paramType, paramValues[i]);
          }
        } else {
          String bodyString = request.getBodyString();
          if (StrUtil.isNotBlank(bodyString)) {
            injectRequestJson(classMethodaccessMap, paramnames, paramValues, i, paramType, bodyString);
          }
        }
      }
    }

    MethodAccess methodAccess = TioBootHttpControllerRoute.BEAN_METHODACCESS_MAP.get(controllerBean);
    actionRetrunValue = methodAccess.invoke(controllerBean, actionMethod.getName(), parameterTypes, paramValues);
    return actionRetrunValue;
  }

  /**
   * 
   * @param classMethodaccessMap
   * @param paramnames
   * @param paramValues
   * @param i
   * @param paramType
   * @param bodyString
   */
  private void injectRequestJson(Map<Class<?>, MethodAccess> classMethodaccessMap, String[] paramnames,
      Object[] paramValues, int i, Class<?> paramType, String bodyString) {
    try {
      // 检查是否为简单类型或数组，如果是则需要特殊处理；这里我们主要处理Json转换为对象
      if (!ClassUtils.isSimpleTypeOrArray(paramType)) {
        Object parsedObject = JsonUtils.parse(bodyString, paramType);
        paramValues[i] = parsedObject;
      } else {
        log.error("{}:Attempting to deserialize JSON into a simple type or array, which is not supported directly.",
            paramType);
      }
    } catch (Exception e) {
      log.error("Error while inject request json:{},{}", paramType, paramValues[i]);
    }

  }

  /**
   * 注入请求参数
   * @param classMethodaccessMap
   * @param paramnames
   * @param paramValues
   * @param i
   * @param paramType
   * @param params
   * @throws Exception
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  private void injectRequestParameters(Map<Class<?>, MethodAccess> classMethodaccessMap, String[] paramnames,
      Object[] paramValues, int i, Class<?> paramType, Map<String, Object[]> params)
      throws Exception, InstantiationException, IllegalAccessException {

    if (ClassUtils.isSimpleTypeOrArray(paramType)) {
      Object[] value = params.get(paramnames[i]);
      if (value != null && value.length > 0) {
        if (paramType.isArray()) {
          if (value.getClass() == String[].class) {
            paramValues[i] = StrUtil.convert(paramType, (String[]) value);
          } else {
            paramValues[i] = value;
          }
        } else {
          if (value[0] == null) {
            paramValues[i] = null;
          } else {
            if (value[0].getClass() == String.class) {
              paramValues[i] = StrUtil.convert(paramType, (String) value[0]);
            } else {
              paramValues[i] = value[0];
            }
          }
        }
      }
    } else {
      paramValues[i] = paramType.newInstance();// BeanUtil.mapToBean(params, paramType, true);
      Set<Map.Entry<String, Object[]>> set = params.entrySet();
      label2: for (Map.Entry<String, Object[]> entry : set) {
        try {
          String fieldName = entry.getKey();
          Object[] fieldValue = entry.getValue();

          PropertyDescriptor propertyDescriptor = BeanUtil.getPropertyDescriptor(paramType, fieldName, false);
          if (propertyDescriptor == null) {
            continue label2;
          } else {
            Method writeMethod = propertyDescriptor.getWriteMethod();
            if (writeMethod == null) {
              continue label2;
            }
            writeMethod = ClassUtil.setAccessible(writeMethod);
            Class<?>[] clazzes = writeMethod.getParameterTypes();
            if (clazzes == null || clazzes.length != 1) {
              log.info("方法的参数长度不为1，{}.{}", paramType.getName(), writeMethod.getName());
              continue label2;
            }
            Class<?> clazz = clazzes[0];

            if (ClassUtils.isSimpleTypeOrArray(clazz)) {
              if (fieldValue != null && fieldValue.length > 0) {
                if (clazz.isArray()) {
                  Object theValue = null;// Convert.convert(clazz, fieldValue);
                  if (fieldValue.getClass() == String[].class) {
                    theValue = StrUtil.convert(clazz, (String[]) fieldValue);
                  } else {
                    theValue = fieldValue;
                  }

                  getMethodAccess(classMethodaccessMap, paramType).invoke(paramValues[i], writeMethod.getName(),
                      theValue);
                } else {
                  Object theValue = null;// Convert.convert(clazz, fieldValue[0]);
                  if (fieldValue[0] == null) {
                    theValue = fieldValue[0];
                  } else {
                    if (fieldValue[0].getClass() == String.class) {
                      theValue = StrUtil.convert(clazz, (String) fieldValue[0]);
                    } else {
                      theValue = fieldValue[0];
                    }
                  }

                  getMethodAccess(classMethodaccessMap, paramType).invoke(paramValues[i], writeMethod.getName(),
                      theValue);
                }
              }
            }
          }
        } catch (Throwable e) {
          log.error(e.toString(), e);
        }
      }
    }
  }

  /**
   * 
   * @param request
   * @param response
   * @param obj
   * @return
   */
  private HttpResponse afterExecuteAction(HttpRequest request, Object actionRetrunValue) {
    HttpResponse response = TioRequestContext.getResponse();

    if (actionRetrunValue == null) {
      return response;
    }
    if (actionRetrunValue instanceof HttpResponse) {
      // action return http response
      response = (HttpResponse) actionRetrunValue;
    } else if (actionRetrunValue instanceof String) {
      // action return string
      response = Resps.txt(response, (String) actionRetrunValue);
    } else if (actionRetrunValue instanceof Integer) {
      response = Resps.txt(response, (String) actionRetrunValue);

    } else if (actionRetrunValue instanceof Long) {
      response = Resps.txt(response, (String) actionRetrunValue);

    } else if (actionRetrunValue instanceof byte[]) { // 字节类型
      response.setBody((byte[]) actionRetrunValue);

    } else if (actionRetrunValue instanceof Template) {
      // action return Template
      Map<Object, Object> data = new HashMap<Object, Object>();
      for (Enumeration<String> attrs = request.getAttributeNames(); attrs.hasMoreElements();) {
        String attrName = attrs.nextElement();
        data.put(attrName, request.getAttribute(attrName));
      }

      String renderToString = ((Template) actionRetrunValue).renderToString(data);

      response = Resps.html(response, renderToString);
    } else {
      // action return 其他值
      response = Resps.json(response, actionRetrunValue);
    }

    return response;
  }
}
