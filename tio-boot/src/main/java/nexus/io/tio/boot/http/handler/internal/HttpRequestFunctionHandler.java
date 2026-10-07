package nexus.io.tio.boot.http.handler.internal;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import nexus.io.annotation.EnableCORS;
import nexus.io.model.type.TioTypeReference;
import nexus.io.tio.boot.http.utils.TioActionResponseProcessor;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.handler.IHttpRequestFunction;
import nexus.io.tio.http.server.handler.RouteEntry;
import nexus.io.tio.http.server.model.HttpCors;
import nexus.io.tio.http.server.util.CORSUtils;
import nexus.io.tio.utils.json.JsonUtils;
import nexus.io.model.exception.ParameterValidationException;
import nexus.io.tio.utils.validator.ParameterValidator;

/** Binds request bodies and invokes registered HTTP functions. */
public class HttpRequestFunctionHandler {

  @SuppressWarnings("unchecked")
  public <T> HttpResponse handleFunction(HttpRequest request, HttpConfig httpConfig, boolean compatibilityAssignment,
      RouteEntry<?, ?> routeEntry, String path) {
    if (routeEntry == null) {
      throw new IllegalArgumentException("No route found for path: " + path);
    }

    IHttpRequestFunction<?, ?> function = routeEntry.getFunction();
    TioTypeReference<?> typeReference = routeEntry.getTypeReference();
    Type type = typeReference.getType();
    Object input = bindInput(request, type);
    Object result;
    try {
      result = ((IHttpRequestFunction<Object, Object>) function).handle(input);
    } catch (RuntimeException error) {
      // Preserve business and validation exceptions for the global exception handler.
      throw error;
    } catch (Exception error) {
      throw new RuntimeException("Error invoking request function", error);
    }

    HttpResponse response = TioActionResponseProcessor.afterExecuteAction(result);
    EnableCORS enableCORS = findCors(function, type);
    if (enableCORS != null) {
      CORSUtils.enableCORS(response, new HttpCors(enableCORS));
    }
    return response;
  }

  private Object bindInput(HttpRequest request, Type type) {
    byte[] bodyBytes = request.getBody();
    ParameterValidator.require(bodyBytes != null && bodyBytes.length > 0, "Request body is required");
    if (type == byte[].class) {
      return bodyBytes;
    }
    String body = request.getBodyString();
    if (type == String.class) {
      ParameterValidator.require(body != null && !body.trim().isEmpty(), "Request body is required");
      return body;
    }
    try {
      if (type == Integer.class) {
        return Integer.valueOf(body);
      }
      if (type == Long.class) {
        return Long.valueOf(body);
      }
      if (type == Double.class) {
        Double value = Double.valueOf(body);
        ParameterValidator.require(!value.isNaN() && !value.isInfinite(), "Finite number required");
        return value;
      }
      if (type == Float.class) {
        Float value = Float.valueOf(body);
        ParameterValidator.require(!value.isNaN() && !value.isInfinite(), "Finite number required");
        return value;
      }
      if (type == Boolean.class) {
        ParameterValidator.require("true".equalsIgnoreCase(body) || "false".equalsIgnoreCase(body),
            "Boolean body must be true or false");
        return Boolean.valueOf(body);
      }
      if (type == Byte.class) {
        return Byte.valueOf(body);
      }
      if (type == Short.class) {
        return Short.valueOf(body);
      }
      if (type == Character.class) {
        ParameterValidator.require(body != null && body.length() == 1, "Exactly one character required");
        return body.charAt(0);
      }
      Object input = JsonUtils.parse(bodyBytes, type);
      ParameterValidator.require(input != null, "Request body must not be null");
      return input;
    } catch (ParameterValidationException error) {
      throw error;
    } catch (RuntimeException error) {
      ParameterValidationException invalid = new ParameterValidationException("Invalid request body");
      invalid.initCause(error);
      throw invalid;
    }
  }

  private EnableCORS findCors(IHttpRequestFunction<?, ?> function, Type type) {
    Class<?> functionClass = function.getClass();
    Type rawType = type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType() : type;
    if (rawType instanceof Class<?>) {
      EnableCORS annotation = methodCors(functionClass, (Class<?>) rawType);
      if (annotation != null) {
        return annotation;
      }
    }
    EnableCORS annotation = methodCors(functionClass, Object.class);
    if (annotation != null) {
      return annotation;
    }
    // Lambda and method-reference target annotations are not carried by the function object.
    return functionClass.getAnnotation(EnableCORS.class);
  }

  private EnableCORS methodCors(Class<?> functionClass, Class<?> parameterType) {
    try {
      // Public lookup includes inherited implementations and bridge methods.
      Method method = functionClass.getMethod("handle", parameterType);
      return method.getAnnotation(EnableCORS.class);
    } catch (NoSuchMethodException error) {
      return null;
    }
  }
}
