package nexus.io.tio.http.server.router;

import java.util.*;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

/** Matching is side-effect free; dispatchers apply parameters only after selecting a route. */
public final class RouteMatch {
  public enum Status { MATCHED, NOT_FOUND, METHOD_NOT_ALLOWED }
  private final Status status;
  private final RouteDefinition route;
  private final Map<String, String> parameters;
  private final Set<HttpMethod> allowedMethods;

  public RouteMatch(Status status, RouteDefinition route, Map<String, String> parameters, Set<HttpMethod> allowed) {
    this.status = status;
    this.route = route;
    this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    this.allowedMethods = Collections.unmodifiableSet(new LinkedHashSet<>(allowed));
  }
  public Status getStatus() { return status; }
  public RouteDefinition getRoute() { return route; }
  public Map<String, String> getParameters() { return parameters; }
  public Set<HttpMethod> getAllowedMethods() { return allowedMethods; }
  public String getAllowHeader() {
    List<String> names = new ArrayList<>();
    for (HttpMethod method : allowedMethods) names.add(method.name());
    return String.join(", ", names);
  }
  public void apply(HttpRequest request) {
    // A query/body field must not shadow the selected URL's path identity.
    for (Map.Entry<String, String> entry : parameters.entrySet())
      request.getParams().put(entry.getKey(), new String[] { entry.getValue() });
  }
  public HttpResponse methodNotAllowed(HttpRequest request) {
    HttpResponse response = new HttpResponse(request);
    response.setStatus(405);
    response.addHeader("Allow", getAllowHeader());
    return response;
  }
}
