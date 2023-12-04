package com.litongjava.tio.boot.httphandler;

import com.esotericsoftware.reflectasm.MethodAccess;
import lombok.extern.slf4j.Slf4j;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.session.HttpSession;
import org.tio.http.server.mvc.Routes;
import org.tio.http.server.util.ClassUtils;
import org.tio.http.server.util.Resps;
import org.tio.server.ServerChannelContext;
import org.tio.utils.hutool.BeanUtil;
import org.tio.utils.hutool.ClassUtil;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.lock.LockUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Created by litonglinux@qq.com on 11/9/2023_2:29 AM
 */
@Slf4j
public class HandlerDispatcher {
  public HttpResponse getNotNullMethodHttpResponse(HttpConfig httpConfig, Routes routes, boolean compatibilityAssignment,
                                                   Map<Class<?>, MethodAccess> classMethodaccessMap,
                                                   HttpRequest request, HttpResponse response, Method method) {
    String[] paramnames = routes.METHOD_PARAMNAME_MAP.get(method);
    Class<?>[] parameterTypes = routes.METHOD_PARAMTYPE_MAP.get(method);// method.getParameterTypes();
    Object bean = routes.METHOD_BEAN_MAP.get(method);
    Object obj = null;
    if (parameterTypes == null || parameterTypes.length == 0) {
      obj = Routes.BEAN_METHODACCESS_MAP.get(bean).invoke(bean, method.getName(), parameterTypes, (Object) null);
    } else {
      // 赋值这段代码待重构，先用上
      Object[] paramValues = new Object[parameterTypes.length];
      int i = 0;
      label_3:
      for (Class<?> paramType : parameterTypes) {
        try {
          if (paramType == HttpRequest.class) {
            paramValues[i] = request;
            continue label_3;
          } else {
            if (compatibilityAssignment) {
              if (paramType == HttpSession.class) {
                paramValues[i] = request.getHttpSession();
                continue label_3;
              } else if (paramType == HttpConfig.class) {
                paramValues[i] = httpConfig;
                continue label_3;
              } else if (paramType == ServerChannelContext.class) { // paramType.isAssignableFrom(ServerChannelContext.class)
                paramValues[i] = request.channelContext;
                continue label_3;
              }
            }

            Map<String, Object[]> params = request.getParams();
            if (params != null) {
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
                label2:
                for (Map.Entry<String, Object[]> entry : set) {
                  try {
                    String fieldName = entry.getKey();
                    Object[] fieldValue = entry.getValue();

                    PropertyDescriptor propertyDescriptor = BeanUtil.getPropertyDescriptor(paramType, fieldName,
                      false);
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

                            getMethodAccess(classMethodaccessMap, paramType).invoke(paramValues[i], writeMethod.getName(), theValue);
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

                            getMethodAccess(classMethodaccessMap, paramType).invoke(paramValues[i], writeMethod.getName(), theValue);
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

          }

        } catch (Throwable e) {
          log.error(request.toString(), e);
        } finally {
          i++;
        }
      }

      MethodAccess methodAccess = Routes.BEAN_METHODACCESS_MAP.get(bean);
      obj = methodAccess.invoke(bean, method.getName(), parameterTypes, paramValues);
    }

    if (obj instanceof HttpResponse) {
      response = (HttpResponse) obj;
      return response;
    } else {
      if (obj == null) {
        if (method.getReturnType() == HttpResponse.class) {
          return null;
        } else {
          response = Resps.json(request, obj);
        }
      } else {
        response = Resps.json(request, obj);
      }
      return response;
    }
  }

  private MethodAccess getMethodAccess(Map<Class<?>, MethodAccess> classMethodaccessMap, Class<?> clazz) throws Exception {
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
}
