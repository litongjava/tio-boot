package com.litongjava.tio.boot.http.interceptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拦击器配置类
 * @author Tong Li
 *
 */
public class HttpInteceptorConfigure {
  Map<String, HttpInterceptorModel> inteceptors = Collections.synchronizedMap(new LinkedHashMap<>());

  public void add(HttpInterceptorModel model) {
    inteceptors.put(model.getName(), model);
  }

  public HttpInterceptorModel remove(String key) {
    return inteceptors.remove(key);
  }

  public Map<String, HttpInterceptorModel> getInteceptors() {
    return inteceptors;
  }

}
