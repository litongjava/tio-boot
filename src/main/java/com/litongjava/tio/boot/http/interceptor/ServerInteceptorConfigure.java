package com.litongjava.tio.boot.http.interceptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拦击器配置类
 * @author Tong Li
 *
 */
public class ServerInteceptorConfigure {
  Map<String, HttpServerInterceptorModel> inteceptors = Collections.synchronizedMap(new LinkedHashMap<>());

  public void add(HttpServerInterceptorModel model) {
    inteceptors.put(model.getName(), model);
  }

  public HttpServerInterceptorModel remove(String key) {
    return inteceptors.remove(key);
  }

  public Map<String, HttpServerInterceptorModel> getInteceptors() {
    return inteceptors;
  }

}
