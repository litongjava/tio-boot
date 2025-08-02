package com.litongjava.tio.boot.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.tio.http.server.util.ClassUtils;
import com.litongjava.tio.utils.hutool.BeanUtil;
import com.litongjava.tio.utils.hutool.ClassUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lock.LockUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioAsmUtils {

  private static Map<Class<?>, MethodAccess> classMethodAccessMap = new HashMap<>();

  /**
   * 获取class中可以访问的方法
   * 
   * @param classMethodaccessMap
   * @param clazz
   * @return
   * @throws Exception
   */
  public static MethodAccess getMethodAccess(Map<Class<?>, MethodAccess> classMethodaccessMap, Class<?> clazz) throws Exception {
    MethodAccess ret = classMethodaccessMap.get(clazz);
    if (ret == null) {
      LockUtils.runWriteOrWaitRead("_tio_http_h_ma_" + clazz.getName(), clazz, () -> {
        if (classMethodaccessMap.get(clazz) == null) {
          classMethodaccessMap.put(clazz, MethodAccess.get(clazz));
        }
      });
      ret = classMethodaccessMap.get(clazz);
    }
    return ret;
  }

  /**
   * 
   * @param params
   * @param i
   * @param paramName
   * @param paramType
   * @param paramValues
   */
  @SuppressWarnings("deprecation")
  public static void injectParametersIntoObject(Map<String, Object[]> params, int i, String paramName, Class<?> paramType, Object[] paramValues) {
    if (ClassUtils.isSimpleTypeOrArray(paramType)) {
      Object[] value = params.get(paramName);
      if (value != null && value.length > 0) {
        if (paramType.isArray()) {
          if (value.getClass() == String[].class) {
            try {
              paramValues[i] = StrUtil.convert(paramType, (String[]) value);
            } catch (Exception e) {
              e.printStackTrace();
            }
          } else {
            paramValues[i] = value;
          }
        } else {
          if (value[0] == null) {
            paramValues[i] = null;
          } else {
            if (value[0].getClass() == String.class) {
              try {
                paramValues[i] = StrUtil.convert(paramType, (String) value[0]);
              } catch (Exception e) {
                e.printStackTrace();
              }
            } else {
              paramValues[i] = value[0];
            }
          }
        }
      }
    } else {
      try {
        paramValues[i] = paramType.newInstance();
      } catch (InstantiationException e1) {
        e1.printStackTrace();
      } catch (IllegalAccessException e1) {
        e1.printStackTrace();
      }
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
              for (Method method : paramType.getMethods()) {
                if (method.getName().equals(propertyDescriptor.getName()) || method.getName().equals("set" + StrUtil.upperFirst(propertyDescriptor.getName()))) {
                  if (method.getParameterCount() == 1) {
                    writeMethod = method;
                    break;
                  }
                }
              }
              if (writeMethod == null) {
                continue label2;
              }
              
            }
            writeMethod = ClassUtil.setAccessible(writeMethod);
            Class<?>[] clazzes = writeMethod.getParameterTypes();
            if (clazzes == null || clazzes.length != 1) {
              log.info("The length of the method parameters is not 1:{}.{}", paramType.getName(), writeMethod.getName());
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

                  TioAsmUtils.getMethodAccess(classMethodAccessMap, paramType).invoke(paramValues[i], writeMethod.getName(), theValue);
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

                  TioAsmUtils.getMethodAccess(classMethodAccessMap, paramType).invoke(paramValues[i], writeMethod.getName(), theValue);
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
