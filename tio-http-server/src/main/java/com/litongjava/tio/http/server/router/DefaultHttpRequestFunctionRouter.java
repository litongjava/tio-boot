package com.litongjava.tio.http.server.router;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.litongjava.model.type.TioTypeReference;
import com.litongjava.tio.http.server.handler.IHttpRequestFunction;
import com.litongjava.tio.http.server.handler.RouteEntry;

public class DefaultHttpRequestFunctionRouter implements HttpRequestFunctionRouter {

  // RouteEntry 用于保存函数及其类型引用
  private final Map<String, RouteEntry<?, ?>> requestMapping = new ConcurrentHashMap<>();

  @Override
  public <R, T> void add(String path, IHttpRequestFunction<R, T> function, TioTypeReference<T> typeReference) {
    requestMapping.put(path, new RouteEntry<>(function, typeReference));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R, T> RouteEntry<R, T> find(String path) {
    RouteEntry<?, ?> entry = requestMapping.get(path);
    if (entry != null) {
      try {
        return (RouteEntry<R, T>) entry;
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Function type mismatch for path: " + path, e);
      }
    }

    // Check for wildcard matches
    Set<Map.Entry<String, RouteEntry<?, ?>>> entrySet = requestMapping.entrySet();

    for (Map.Entry<String, RouteEntry<?, ?>> mapEntry : entrySet) {
      String key = mapEntry.getKey();

      if (key.endsWith("/*")) {
        String baseRoute = key.substring(0, key.length() - 1);
        if (path.startsWith(baseRoute)) {
          return (RouteEntry<R, T>) mapEntry.getValue(); // 强制类型转换
        }
      } else if (key.endsWith("/**")) {
        String baseRoute = key.substring(0, key.length() - 2);
        if (path.startsWith(baseRoute)) {
          return (RouteEntry<R, T>) mapEntry.getValue(); // 强制类型转换
        }
      }
    }

    return null;
  }
}
