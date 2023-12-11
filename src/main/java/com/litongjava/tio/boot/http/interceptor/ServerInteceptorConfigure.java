package com.litongjava.tio.boot.http.interceptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.litongjava.tio.http.server.intf.HttpServerInterceptor;

public class ServerInteceptorConfigure {
  Map<String, Class<? extends HttpServerInterceptor>> inteceptors = Collections.synchronizedMap(new LinkedHashMap<>());

  public void add(String path, Class<? extends HttpServerInterceptor> clazz) {
    inteceptors.put(path, clazz);
  }

  public Class<? extends HttpServerInterceptor> remove(String path) {
    return inteceptors.remove(path);
  }

  public Map<String, Class<? extends HttpServerInterceptor>> getInteceptors() {
    return inteceptors;
  }

}
