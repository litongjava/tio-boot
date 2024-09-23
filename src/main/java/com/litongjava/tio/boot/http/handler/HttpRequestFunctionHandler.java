package com.litongjava.tio.boot.http.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.litongjava.model.type.TioTypeReference;
import com.litongjava.tio.boot.http.utils.RequestActionUtils;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.annotation.EnableCORS;
import com.litongjava.tio.http.server.handler.IHttpRequestFunction;
import com.litongjava.tio.http.server.handler.RouteEntry;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class HttpRequestFunctionHandler {

  @SuppressWarnings("unchecked")
  public <T> HttpResponse handleFunction(HttpRequest request, HttpConfig httpConfig, boolean compatibilityAssignment, RouteEntry<?, ?> routeEntry, String path) {
    if (routeEntry == null) {
      throw new RuntimeException("No route found for path: " + path);
    }

    IHttpRequestFunction<?, ?> function = routeEntry.getFunction();
    TioTypeReference<?> typeReference = routeEntry.getTypeReference();
    Type type = typeReference.getType();

    Object result = null;
    try {
      // 处理数组类型
      if (type == byte[].class) {
        // 如果泛型 T 是 byte[]
        byte[] body = request.getBody();
        result = ((IHttpRequestFunction<Object, byte[]>) function).handle(body); // 强制类型转换为 byte[]
      } else if (type == String.class) {
        // 如果泛型 T 是 String
        String bodyString = request.getBodyString();
        result = ((IHttpRequestFunction<Object, String>) function).handle(bodyString);
      } else if (type == Integer.class) {
        // 如果泛型 T 是 Integer
        Integer bodyInt = Integer.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Integer>) function).handle(bodyInt);
      } else if (type == Long.class) {
        // 如果泛型 T 是 Long
        Long bodyLong = Long.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Long>) function).handle(bodyLong);
      } else if (type == Double.class) {
        // 如果泛型 T 是 Double
        Double bodyDouble = Double.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Double>) function).handle(bodyDouble);
      } else if (type == Float.class) {
        // 如果泛型 T 是 Float
        Float bodyFloat = Float.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Float>) function).handle(bodyFloat);
      } else if (type == Boolean.class) {
        // 如果泛型 T 是 Boolean
        Boolean bodyBoolean = Boolean.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Boolean>) function).handle(bodyBoolean);
      } else if (type == Byte.class) {
        // 如果泛型 T 是 Byte
        Byte bodyByte = Byte.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Byte>) function).handle(bodyByte);
      } else if (type == Short.class) {
        // 如果泛型 T 是 Short
        Short bodyShort = Short.valueOf(request.getBodyString());
        result = ((IHttpRequestFunction<Object, Short>) function).handle(bodyShort);
      } else if (type == Character.class) {
        // 如果泛型 T 是 Character
        Character bodyChar = request.getBodyString().charAt(0);
        result = ((IHttpRequestFunction<Object, Character>) function).handle(bodyChar);
      } else {
        // 其他复杂对象使用 JsonUtils 解析
        byte[] body = request.getBody();
        T functionInput = JsonUtils.parse(body, type);
        try {
          // 调用 handle 方法，传递类型为 T 的参数
          result = ((IHttpRequestFunction<Object, T>) function).handle(functionInput);
        } catch (ClassCastException e) {
          throw new RuntimeException("Error casting parsed object to the required type: " + type, e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error processing request", e);
    }

    HttpResponse response = RequestActionUtils.afterExecuteAction(result);

    // 处理 CORS 注解
    boolean isEnableCORS = false;
    EnableCORS enableCORS = null;
    try {
      Method actionMethod = function.getClass().getDeclaredMethod("handle", Object.class); // 假设函数参数为 Object
      enableCORS = actionMethod.getAnnotation(EnableCORS.class);
      if (enableCORS != null) {
        isEnableCORS = true;
      }
      if (!isEnableCORS) {
        // 如果方法没有 CORS 注解，检查类是否有 CORS 注解
        Class<?> clazz = function.getClass();
        enableCORS = clazz.getAnnotation(EnableCORS.class);
        if (enableCORS != null) {
          isEnableCORS = true;
        }
      }
    } catch (NoSuchMethodException ex) {
      throw new RuntimeException("Error accessing method handle in IHttpRequestFunction", ex);
    }

    // 如果有 CORS 注解，启用 CORS 支持
    if (isEnableCORS) {
      CORSUtils.enableCORS(response, new HttpCors(enableCORS));
    }

    return response;
  }

}
