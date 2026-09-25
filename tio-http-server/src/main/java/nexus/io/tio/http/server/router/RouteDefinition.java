package nexus.io.tio.http.server.router;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.server.handler.HttpRequestHandler;

/** Immutable registration. A null method denotes the legacy ANY registration. */
public final class RouteDefinition {
  private final HttpMethod method;
  private final String path;
  private final HttpRequestHandler handler;
  private final Map<String, Object> metadata;

  public RouteDefinition(HttpMethod method, String path, HttpRequestHandler handler, Map<String, ?> metadata) {
    this.method = method;
    this.path = Objects.requireNonNull(path, "path");
    this.handler = Objects.requireNonNull(handler, "handler");
    this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
  }
  public HttpMethod getMethod() { return method; }
  public String getPath() { return path; }
  public HttpRequestHandler getHandler() { return handler; }
  public Map<String, Object> getMetadata() { return metadata; }
}
