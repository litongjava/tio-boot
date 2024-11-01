package com.litongjava.tio.boot.http.handler.controller;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.litongjava.annotation.Delete;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.Post;
import com.litongjava.annotation.Put;
import com.litongjava.annotation.RequestPath;
import com.litongjava.constatns.ServerConfigKeys;
import com.litongjava.controller.ControllerFactory;
import com.litongjava.controller.DefaultControllerFactory;
import com.litongjava.controller.PathUnitVo;
import com.litongjava.controller.VariablePathVo;
import com.litongjava.tio.boot.utils.ParameterNameUtil;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.ArrayUtil;
import com.litongjava.tio.utils.hutool.ClassScanAnnotationHandler;
import com.litongjava.tio.utils.hutool.ClassUtil;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.MapJsonUtils;

/**
 * @author tanyaowu 2017年7月1日 上午9:05:30
 */
public class TioBootHttpControllerRouter {
  private static Logger log = LoggerFactory.getLogger(TioBootHttpControllerRouter.class);

  public static final String META_PATH_KEY = "TIO_HTTP_META_PATH";

  /**
   * 路径和对象映射<br>
   * key: /user<br>
   * value: object<br>  
   */
  public final Map<String, Object> PATH_BEAN_MAP = new TreeMap<>();

  /**
   * class和对象映射<br>
   * key: XxxController.class<br>
   * value: XxxController.class对应的实例对象<br>
   */
  public static final Map<Class<?>, Object> CLASS_BEAN_MAP = new HashMap<>();

  /**
   * bean和MethodAccess映射<br>
   * key: XxxController.class对应的实例对象<br>
   * value: MethodAccess<br>
   */
  public static final Map<Object, MethodAccess> BEAN_METHODACCESS_MAP = new HashMap<>();

  /**
   * 路径和class映射<br>
   * 只是用来打印的<br>
   * key: /user<br>
   * value: Class<br>
   */
  public static final Map<String, Class<?>> PATH_CLASS_MAP = new TreeMap<>();

  /**
   * 路径和class映射<br>
   * key: class<br>
   * value: /user<br>
   */
  public static final Map<Class<?>, String> CLASS_PATH_MAP = new HashMap<>();

  /**
   * Method路径映射<br>
   * key: /user/update，包含forward的路径<br>
   * value: method<br>
   */
  public final Map<String, Method> PATH_METHOD_MAP = new TreeMap<>();

  /**
   * 方法参数名映射<br>
   * key: method<br>
   * value: ["id", "name", "scanPackages"]<br>
   */
  public final Map<Method, String[]> METHOD_PARAM_NAME_MAP = new HashMap<>();

  /**
   * 方法和参数类型映射<br>
   * key: method<br>
   * value: [String.class, int.class]<br>
   */
  public final Map<Method, Class<?>[]> METHOD_PARAM_TYPE_MAP = new HashMap<>();

  /**
   * path跟forward映射<br>
   * key: 原访问路径<br>
   * value: forward后的路径<br>
   * 譬如：原来的访问路径是/user/123，forward是/user/getById，这个相当于是一个rewrite的功能，对外路径要相对友好，对内路径一般用于业务更便捷地处理
   */
  public final Map<String, String> PATH_FORWARD_MAP = new HashMap<>();

  /**
   * 方法和对象映射<br>
   * key: method<br>
   * value: bean<br>
   */
  public final Map<Method, Object> METHOD_BEAN_MAP = new HashMap<>();

  /**
   * Method路径映射<br>
   * 只是用于打印日志<br>
   * key: /user/update<br>
   * value: method string<br>
   */
  public final Map<String, String> PATH_METHOD_STR_MAP = new TreeMap<>();

  /**
   * 含有路径变量的请求<br>
   * value: VariablePathVo<br>
   */
  public final Map<String, VariablePathVo[]> VARIABLE_PATH_MAP = new TreeMap<>();

  /**
   * 含有路径变量的请求<br>
   * 只是用于打印日志<br>
   * key: 配置的路径/user/{userid}<br>
   * value: method string<br>
   */
  public final Map<String, String> VARIABLE_PATH_METHOD_STR_MAP = new TreeMap<>();

  private final StringBuilder errorStr = new StringBuilder();

  private List<Class<?>> scannedClasses = new ArrayList<>();

  public TioBootHttpControllerRouter() {
  }

  /**
   * 
   * @param scanPackages
   */
  public TioBootHttpControllerRouter(String[] scanPackages) {
    this(scanPackages, null);
  }

  public TioBootHttpControllerRouter(String scanPackage) {
    this(scanPackage, null);
  }

  public TioBootHttpControllerRouter(String[] scanPackages, ControllerFactory controllerFactory) {
    addRoutes(scanPackages, controllerFactory);
  }

  public TioBootHttpControllerRouter(String scanPackage, ControllerFactory controllerFactory) {
    this(new String[] { scanPackage }, controllerFactory);
  }

  //
  public TioBootHttpControllerRouter(Class<?>[] scanRootClasses) {
    this(toPackages(scanRootClasses), null);
  }

  public TioBootHttpControllerRouter(Class<?> scanRootClasse) {
    this(scanRootClasse.getPackage().getName(), null);
  }

  public TioBootHttpControllerRouter(Class<?>[] scanRootClasses, ControllerFactory controllerFactory) {
    addRoutes(toPackages(scanRootClasses), controllerFactory);
  }

  public TioBootHttpControllerRouter(Class<?> scanRootClasse, ControllerFactory controllerFactory) {
    this(new String[] { scanRootClasse.getPackage().getName() }, controllerFactory);
  }

  /**
   * 根据 扫描的到的scannedClasses 添加路由
   * 
   * @param scannedClasses
   * @param controllerFactory
   */
  public void addControllers(List<Class<?>> scannedClasses) {
    if (scannedClasses == null || scannedClasses.size() < 1) {

      return;
    } else {
      this.scannedClasses.addAll(scannedClasses);
    }
  }

  public void scan(ControllerFactory controllerFactory) {
    for (Class<?> clazz : scannedClasses) {
      this.processClazz(clazz, controllerFactory);
    }
    scannedClasses.clear();
    this.afterProcessClazz();
  }

  public static String[] toPackages(Class<?>[] scanRootClasses) {
    String[] scanPackages = new String[scanRootClasses.length];
    int i = 0;
    for (Class<?> clazz : scanRootClasses) {
      scanPackages[i++] = clazz.getPackage().getName();
    }
    return scanPackages;
  }

  /**
   * 添加路由
   * 
   * @param scanPackages
   * @author tanyaowu
   */
  public void addRoutes(String[] scanPackages) {
    addRoutes(scanPackages, null);
  }

  /**
   * 添加路由
   * 
   * @param scanPackages
   * @param controllerFactory
   * @author tanyaowu
   */
  public void addRoutes(String[] scanPackages, ControllerFactory controllerFactory) {
    if (controllerFactory == null) {
      controllerFactory = DefaultControllerFactory.me;
    }
    ControllerFactory controllerFactory1 = controllerFactory;
    if (scanPackages != null) {
      for (String pkg : scanPackages) {
        try {
          ClassUtil.scanPackage(pkg, new ClassScanAnnotationHandler(RequestPath.class) {
            @Override
            public void handlerAnnotation(Class<?> clazz) {
              processClazz(clazz, controllerFactory1);
            }

          });
        } catch (Exception e) {
          log.error(e.toString(), e);
        }

      }
      afterProcessClazz();

    }
  }

  public void processClazz(Class<?> clazz, ControllerFactory controllerFactory) {
    String beanPath = null;
    RequestPath requestPath = clazz.getAnnotation(RequestPath.class);
    if (requestPath != null) {
      beanPath = requestPath.value();
    } else {
      Get get = clazz.getAnnotation(Get.class);
      if (get != null) {
        beanPath = get.value();
      } else {
        Post post = clazz.getAnnotation(Post.class);
        if (post != null) {
          beanPath = post.value();
        } else {
          Put put = clazz.getAnnotation(Put.class);
          if (put != null) {
            beanPath = put.value();
          } else {
            Delete delete = clazz.getAnnotation(Delete.class);
            if (delete != null) {
              beanPath = delete.value();
            } else {
              return;
            }
          }
        }

      }
    }
    try {
      Object bean = controllerFactory.getInstance(clazz);
      if (bean != null) {
        MethodAccess access = MethodAccess.get(clazz);
        BEAN_METHODACCESS_MAP.put(bean, access);
      }

      Object obj = PATH_BEAN_MAP.get(beanPath);

      if (obj != null) {
        if (!"".equals(beanPath)) {
          log.error("mapping[{}] already exists in class [{}]", beanPath, obj.getClass().getName());
          errorStr.append("mapping[" + beanPath + "] already exists in class [" + obj.getClass().getName() + "]\r\n\r\n");
        } else {
          PATH_BEAN_MAP.put(beanPath, bean);
          CLASS_BEAN_MAP.put(clazz, bean);
          PATH_CLASS_MAP.put(beanPath, clazz);
          CLASS_PATH_MAP.put(clazz, beanPath);
        }
      }

      Method[] methods = clazz.getDeclaredMethods();
      this.processClazzMethods(bean, beanPath, methods);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * 处理Controller的所有Method
   * 
   * @param bean
   * @param beanPath
   * @param methods
   */
  private void processClazzMethods(Object bean, String beanPath, Method[] methods) {
    c: for (Method method : methods) {
      int modifiers = method.getModifiers();
      if (Modifier.isPrivate(modifiers)) {
        continue c;
      }

      Class<?> returnType = method.getReturnType();
      if (returnType == Void.TYPE) {
        continue c;
      }

      // 检查方法上的注解
      String methodPath = null;
      String httpMethodType = null;
      String forwardPath = null;

      // 处理 @RequestPath
      RequestPath requestPath = method.getAnnotation(RequestPath.class);
      if (requestPath != null) {
        methodPath = requestPath.value();
        forwardPath = requestPath.forward();
      }

      // 处理 @Get
      Get get = method.getAnnotation(Get.class);
      if (get != null) {
        methodPath = get.value();
        forwardPath = get.forward();
        httpMethodType = "GET";
      }

      // 处理 @Post
      Post post = method.getAnnotation(Post.class);
      if (post != null) {
        methodPath = post.value();
        forwardPath = post.forward();
        httpMethodType = "POST";
      }

      // 处理 @Put
      Put put = method.getAnnotation(Put.class);
      if (put != null) {
        methodPath = put.value();
        forwardPath = put.forward();
        httpMethodType = "PUT";
      }

      // 处理 @Delete
      Delete delete = method.getAnnotation(Delete.class);
      if (delete != null) {
        methodPath = delete.value();
        forwardPath = delete.forward();
        httpMethodType = "DELETE";
      }

      // 如果没有任何相关注解，则跳过
      if (methodPath == null) {
        // 默认使用方法名作为路径
        methodPath = "/" + method.getName();
      }

      String completePath = beanPath + methodPath;
      Class<?>[] parameterTypes = method.getParameterTypes();
      try {
        String[] parameterNames = ParameterNameUtil.getParameterNames(method);

        // 检查路径是否已存在
        String key = null;
        String existingMethodStr = null;
        if (httpMethodType == null) {
          key = completePath;
          existingMethodStr = PATH_METHOD_STR_MAP.get(key);
        } else {
          key = httpMethodType + " " + completePath;
          existingMethodStr = PATH_METHOD_STR_MAP.get(key);
        }

        if (existingMethodStr != null) {
          log.error("mapping[{}] already exists in method [{}]", completePath, existingMethodStr);
          errorStr.append("mapping[" + completePath + "] already exists in method [" + existingMethodStr + "]\r\n\r\n");
          continue c;
        }

        // 构建方法字符串
        String methodStr = methodToStr(method, parameterNames);

        // 根据 HTTP 方法类型存储不同的映射
        PATH_METHOD_MAP.put(key, method);
        PATH_METHOD_STR_MAP.put(key, methodStr);

        METHOD_PARAM_NAME_MAP.put(method, parameterNames);
        METHOD_PARAM_TYPE_MAP.put(method, parameterTypes);
        if (forwardPath != null && !forwardPath.trim().isEmpty()) {
          String forwardKey = httpMethodType + " " + forwardPath;
          PATH_FORWARD_MAP.put(key, forwardPath);
          PATH_METHOD_STR_MAP.put(forwardKey, methodStr);
          PATH_METHOD_MAP.put(forwardKey, method);
        }

        METHOD_BEAN_MAP.put(method, bean);
      } catch (Throwable e) {
        log.error(e.toString(), e);
      }
    }
  }

  public void afterProcessClazz() {

    processVariablePath();
    printMapping();

  }

  private void printMapping() {
    String pathClassMapStr = MapJsonUtils.toPrettyJson(PATH_CLASS_MAP);
    String pathMethodstrMapStr = MapJsonUtils.toPrettyJson(PATH_METHOD_STR_MAP);
    String variablePathMethodstrMapStr = MapJsonUtils.toPrettyJson(VARIABLE_PATH_METHOD_STR_MAP);
    if (EnvUtils.getBoolean(ServerConfigKeys.SERVER_HTTP_CONTROLLER_PRINTMAPPING, true)) {
      if (PATH_CLASS_MAP.size() > 0) {
        log.info("class  mapping\r\n{}", pathClassMapStr);
      }

      if (PATH_METHOD_STR_MAP.size() > 0) {

        log.info("method mapping\r\n{}", pathMethodstrMapStr);
      }

      if (VARIABLE_PATH_METHOD_STR_MAP.size() > 0) {
        log.info("variable path mapping\r\n{}", variablePathMethodstrMapStr);
      }

    }

    if (EnvUtils.getBoolean(ServerConfigKeys.SERVER_HTTP_CONTROLLER_WRITEMAPPING, false)) {
      try {
        FileUtil.writeString(pathClassMapStr, "tio_boot_path_class.json", "utf-8");
        FileUtil.writeString(pathMethodstrMapStr, "tio_boot_path_method.json", "utf-8");
        FileUtil.writeString(variablePathMethodstrMapStr, "tio_boot_variablepath_method.json", "utf-8");
        if (errorStr.length() > 0) {
          FileUtil.writeString(errorStr.toString(), "tio_boot_error.txt", "utf-8");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 处理有变量的路径
   * 
   * @param PATH_METHOD_MAP
   */
  private void processVariablePath() {
    Set<Entry<String, Method>> set = PATH_METHOD_MAP.entrySet();
    for (Entry<String, Method> entry : set) {
      String key = entry.getKey(); // e.g., "PUT /users/{id}"
      Method method = entry.getValue();

      if (StrUtil.contains(key, '{') && StrUtil.contains(key, '}')) {

        // Extract HTTP method and path
        String httpMethod = null;
        String path = null;
        int spaceIndex = key.indexOf(' ');
        if (spaceIndex == -1) {
          path = key;
        } else {
          httpMethod = key.substring(0, spaceIndex).toUpperCase();
          path = key.substring(spaceIndex + 1);
        }

        String[] pathUnits = StrUtil.split(path, "/");
        PathUnitVo[] pathUnitVos = new PathUnitVo[pathUnits.length];

        boolean isVarPath = false;
        for (int i = 0; i < pathUnits.length; i++) {
          PathUnitVo pathUnitVo = new PathUnitVo();
          String pathUnit = pathUnits[i];
          if (StrUtil.contains(pathUnit, '{') || StrUtil.contains(pathUnit, '}')) {
            if (StrUtil.startWith(pathUnit, "{") && StrUtil.endWith(pathUnit, "}")) {
              String varName = StrUtil.subBetween(pathUnit, "{", "}");
              if (ArrayUtil.contains(METHOD_PARAM_NAME_MAP.get(method), varName)) {
                isVarPath = true;
                pathUnitVo.setVar(true);
                pathUnitVo.setPath(varName);
              } else {
                log.error("path:{}, method [{}] does not contain parameter named {}", path, method, varName);
                errorStr.append("path:{" + path + "}, method [" + method + "] does not contain parameter named " + varName + "\r\n\r\n");
              }
            } else {
              pathUnitVo.setVar(false);
              pathUnitVo.setPath(pathUnit);
            }
          } else {
            pathUnitVo.setVar(false);
            pathUnitVo.setPath(pathUnit);
          }
          pathUnitVos[i] = pathUnitVo;
        }

        if (isVarPath) {
          VariablePathVo variablePathVo = new VariablePathVo(path, method, pathUnitVos);
          addVariablePathVo(httpMethod, pathUnits.length, variablePathVo);
        }
      }
    }
  }

  /**
   * 根据class获取class对应的bean
   * 
   * @param <T>
   * @param clazz
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T getController(Class<T> clazz) {
    return (T) CLASS_BEAN_MAP.get(clazz);
  }

  public static String getRequestPath(Class<?> clazz) {
    return CLASS_PATH_MAP.get(clazz);
  }

  /**
   * @param httpMethod
   * @param pathUnitCount
   * @param variablePathVo
   */
  private void addVariablePathVo(String httpMethod, Integer pathUnitCount, VariablePathVo variablePathVo) {
    String key = null;
    if (httpMethod != null) {
      key = httpMethod.toUpperCase() + " " + pathUnitCount;
    } else {
      key = pathUnitCount + "";
    }

    VariablePathVo[] existValue = VARIABLE_PATH_MAP.get(key);
    if (existValue == null) {
      existValue = new VariablePathVo[] { variablePathVo };
      VARIABLE_PATH_MAP.put(key, existValue);
    } else {
      VariablePathVo[] newExistValue = new VariablePathVo[existValue.length + 1];
      System.arraycopy(existValue, 0, newExistValue, 0, existValue.length);
      newExistValue[newExistValue.length - 1] = variablePathVo;
      VARIABLE_PATH_MAP.put(key, newExistValue);
    }
    String methodStr = methodToStr(variablePathVo.getMethod(), METHOD_PARAM_NAME_MAP.get(variablePathVo.getMethod()));
    if (httpMethod != null) {
      VARIABLE_PATH_METHOD_STR_MAP.put(httpMethod + " " + variablePathVo.getPath(), methodStr);
    } else {
      VARIABLE_PATH_METHOD_STR_MAP.put(variablePathVo.getPath(), methodStr);
    }

  }

  private String methodToStr(Method method, String[] parameterNames) {
    return method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.join(parameterNames, ",") + ")";
  }

  public Method getActionByPath(String path, String httpMethod, HttpRequest request) {

    String key = httpMethod.toUpperCase() + " " + path;
    Method method = PATH_METHOD_MAP.get(key);
    if (method != null) {
      return method;
    }

    method = PATH_METHOD_MAP.get(path);
    if (method != null) {
      return method;
    }

    String[] pathUnitsOfRequest = StrUtil.split(path, "/"); // e.g., ["", "users", "1"]
    String varPathKey = httpMethod.toUpperCase() + " " + pathUnitsOfRequest.length;
    VariablePathVo[] variablePathVos = VARIABLE_PATH_MAP.get(varPathKey);

    if (variablePathVos == null) {
      variablePathVos = VARIABLE_PATH_MAP.get(pathUnitsOfRequest.length + "");
    }

    if (variablePathVos != null) {
      for (VariablePathVo variablePathVo : variablePathVos) {
        PathUnitVo[] pathUnitVos = variablePathVo.getPathUnits();

        boolean isMatch = true;
        for (int i = 0; i < pathUnitVos.length; i++) {
          PathUnitVo pathUnitVo = pathUnitVos[i];
          String pathUnitOfRequest = pathUnitsOfRequest[i];
          String pathOfVo = pathUnitVo.getPath();

          if (pathUnitVo.isVar()) {
            request.addParam(pathOfVo, pathUnitOfRequest);
          } else {
            if (!StrUtil.equals(pathOfVo, pathUnitOfRequest)) {
              isMatch = false;
              break;
            }
          }
        }

        if (isMatch) {
          String metapath = variablePathVo.getPath();
          String forward = PATH_FORWARD_MAP.get(httpMethod.toUpperCase() + " " + metapath);
          if (StrUtil.isNotBlank(forward)) {
            request.requestLine.path = forward;
          }
          method = variablePathVo.getMethod();
          return method;
        }
      }
    }
    return null;
  }
}