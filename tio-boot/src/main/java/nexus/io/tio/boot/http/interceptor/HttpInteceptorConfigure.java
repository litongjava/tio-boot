package nexus.io.tio.boot.http.interceptor;

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
    if (model.getName() == null || model.getName().trim().isEmpty()) {
      model.setName("interceptor-" + java.util.UUID.randomUUID().toString());
    }
    inteceptors.put(model.getName(), model);
  }

  public java.util.List<HttpInterceptorModel> snapshot() {
    synchronized (inteceptors) {
      return new java.util.ArrayList<>(inteceptors.values());
    }
  }

  public HttpInterceptorModel remove(String key) {
    return inteceptors.remove(key);
  }

  public Map<String, HttpInterceptorModel> getInteceptors() {
    return inteceptors;
  }

}
