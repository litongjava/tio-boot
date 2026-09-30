package nexus.io.tio.http.server.router;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import nexus.io.tio.http.common.HttpMethod;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.handler.HttpRequestHandler;

public interface HttpRequestRouter {

  default void add(HttpMethod method, String path, HttpRequestHandler handler) {
    add(method, path, handler, Collections.emptyMap());
  }

  default void add(HttpMethod method, String path, HttpRequestHandler handler, Map<String, ?> metadata) {
    throw new UnsupportedOperationException("This router does not support method-aware registrations");
  }

  default void get(String path, HttpRequestHandler handler) { add(HttpMethod.GET, path, handler); }
  default void post(String path, HttpRequestHandler handler) { add(HttpMethod.POST, path, handler); }
  default void put(String path, HttpRequestHandler handler) { add(HttpMethod.PUT, path, handler); }
  default void delete(String path, HttpRequestHandler handler) { add(HttpMethod.DELETE, path, handler); }
  default void patch(String path, HttpRequestHandler handler) { add(HttpMethod.PATCH, path, handler); }
  default void options(String path, HttpRequestHandler handler) { add(HttpMethod.OPTIONS, path, handler); }
  default void head(String path, HttpRequestHandler handler) { add(HttpMethod.HEAD, path, handler); }

  /** Automatic OPTIONS for known resources and the server-wide asterisk target. */
  default HttpResponse automaticOptions(HttpRequest request) {
    if (request.getRequestLine().getMethod() != HttpMethod.OPTIONS) return null;
    RouteMatch match = match(request);
    if (match.getStatus() == RouteMatch.Status.MATCHED) return null;
    if ("*".equals(request.getRequestURI())) {
      java.util.Set<HttpMethod> methods = java.util.EnumSet.of(HttpMethod.OPTIONS);
      for (RouteDefinition route : allRoutes()) {
        if (route.getMethod() != null) methods.add(route.getMethod());
      }
      if (methods.contains(HttpMethod.GET)) methods.add(HttpMethod.HEAD);
      match = new RouteMatch(RouteMatch.Status.METHOD_NOT_ALLOWED, null,
          Collections.emptyMap(), methods);
    } else if (match.getStatus() != RouteMatch.Status.METHOD_NOT_ALLOWED) {
      return null;
    }
    return new HttpResponse(request).setStatus(204).addHeader("Allow", match.getAllowHeader());
  }

  /** Compatibility adapter for third-party legacy routers. */
  default RouteMatch match(HttpRequest request) {
    HttpRequestHandler handler = resolve(request);
    return new RouteMatch(handler == null ? RouteMatch.Status.NOT_FOUND : RouteMatch.Status.MATCHED,
        handler == null ? null : new RouteDefinition(null, request.getRequestURI(), handler, Collections.emptyMap()),
        Collections.emptyMap(), Collections.emptySet());
  }

  default List<RouteDefinition> allRoutes() {
    List<RouteDefinition> result = new ArrayList<>();
    for (Map.Entry<String, HttpRequestHandler> entry : all().entrySet())
      result.add(new RouteDefinition(null, entry.getKey(), entry.getValue(), Collections.emptyMap()));
    return Collections.unmodifiableList(result);
  }

  /**
   * 添加 GET、POST、PUT、DELETE 路由。PATCH 等其他业务方法需单独注册。
   * HEAD 自动回退到 GET；OPTIONS 由分发器自动响应，也可显式注册覆盖。
   * 
   * @param path
   * @param handler
   */
  public void add(String path, HttpRequestHandler handler);

  /**
   * 查找路由
   * 
   * @param path
   * @return
   */
  public HttpRequestHandler find(String path);

  public Map<String, HttpRequestHandler> all();

  default HttpRequestHandler resolve(HttpRequest request) {
    return find(request.getRequestURI());
  }
}
