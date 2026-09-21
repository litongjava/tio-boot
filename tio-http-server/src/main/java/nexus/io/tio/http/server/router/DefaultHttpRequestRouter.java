package nexus.io.tio.http.server.router;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.server.handler.HttpRequestHandler;

public class DefaultHttpRequestRouter implements HttpRequestRouter {
  private final Map<String, HttpRequestHandler> requestMapping = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();
  private volatile List<Entry> orderedEntries = Collections.emptyList();

  private static final class Entry {
    final RouteDefinition definition;
    final Route template;
    Entry(RouteDefinition definition, Route template) { this.definition = definition; this.template = template; }
    int priority() {
      String path = definition.getPath();
      return template != null ? 2 : path.endsWith("/*") || path.endsWith("/**") ? 1 : 0;
    }
  }

  @Override
  public synchronized void add(String path, HttpRequestHandler handler) {
    register(null, path, handler, Collections.emptyMap());
    if (!(path.contains("{") && path.contains("}"))) requestMapping.put(path, handler);
  }

  @Override
  public synchronized void add(HttpMethod method, String path, HttpRequestHandler handler, Map<String, ?> metadata) {
    register(Objects.requireNonNull(method, "method"), path, handler, metadata);
  }

  private void register(HttpMethod method, String path, HttpRequestHandler handler, Map<String, ?> metadata) {
    RouteDefinition definition = new RouteDefinition(method, path, handler, metadata);
    Route compiled = path.contains("{") && path.contains("}") ? compileTemplate(path, handler) : null;
    for (Entry entry : entries) {
      if (entry.definition.getMethod() == method && entry.definition.getPath().equals(path)) {
        if (method != null) throw new IllegalArgumentException("Duplicate route: " + method + " " + path);
        entries.remove(entry); // Legacy add replaces the previous registration.
        break;
      }
    }
    entries.add(new Entry(definition, compiled));
    List<Entry> snapshot = new ArrayList<>(entries);
    // Keep legacy exact > wildcard > template precedence; longest wildcard wins deterministically.
    Collections.sort(snapshot, (a, b) -> {
      int order = Integer.compare(a.priority(), b.priority());
      return order != 0 ? order : a.priority() == 1
          ? Integer.compare(b.definition.getPath().length(), a.definition.getPath().length()) : 0;
    });
    orderedEntries = Collections.unmodifiableList(snapshot);
  }

  /** Legacy path-only lookup intentionally sees only legacy ANY routes. */
  @Override
  public HttpRequestHandler find(String path) {
    HttpRequestHandler exact = requestMapping.get(path);
    if (exact != null) return exact;
    String best = null;
    for (String pattern : requestMapping.keySet()) {
      if (wildcardMatches(pattern, path) && (best == null || pattern.length() > best.length()
          || pattern.length() == best.length() && pattern.compareTo(best) < 0)) best = pattern;
    }
    return best == null ? null : requestMapping.get(best);
  }

  @Override
  public RouteMatch match(HttpRequest request) {
    String path = request.getRequestURI();
    HttpMethod method = request.getRequestLine().getMethod();
    List<Entry> candidates = orderedEntries;
    Set<HttpMethod> allowed = EnumSet.noneOf(HttpMethod.class);
    Entry selected = null;
    Map<String, String> selectedParams = Collections.emptyMap();
    int bestMethodRank = Integer.MAX_VALUE;
    for (Entry entry : candidates) {
      Map<String, String> params = new LinkedHashMap<>();
      String pattern = entry.definition.getPath();
      boolean matches = entry.template != null ? matchesTemplate(entry.template, fastSplit(path), params)
          : pattern.equals(path) || wildcardMatches(pattern, path);
      if (!matches) continue;
      HttpMethod registered = entry.definition.getMethod();
      if (registered != null) allowed.add(registered);
      int rank = registered == method ? 0 : method == HttpMethod.HEAD && registered == HttpMethod.GET ? 1
          : registered == null ? 2 : Integer.MAX_VALUE;
      if (rank < bestMethodRank) { selected = entry; selectedParams = params; bestMethodRank = rank; }
    }
    if (allowed.contains(HttpMethod.GET)) allowed.add(HttpMethod.HEAD);
    if (!allowed.isEmpty()) allowed.add(HttpMethod.OPTIONS);
    return new RouteMatch(selected != null ? RouteMatch.Status.MATCHED
        : allowed.isEmpty() ? RouteMatch.Status.NOT_FOUND : RouteMatch.Status.METHOD_NOT_ALLOWED,
        selected == null ? null : selected.definition, selectedParams, allowed);
  }

  private static boolean wildcardMatches(String pattern, String path) {
    int suffix = pattern.endsWith("/**") ? 2 : pattern.endsWith("/*") ? 1 : 0;
    return suffix != 0 && path.startsWith(pattern.substring(0, pattern.length() - suffix));
  }

  @Override
  public HttpRequestHandler resolve(HttpRequest request) {
    RouteMatch match = match(request);
    if (match.getStatus() != RouteMatch.Status.MATCHED) return null;
    match.apply(request);
    return match.getRoute().getHandler();
  }

  @Override
  public Map<String, HttpRequestHandler> all() { return Collections.unmodifiableMap(requestMapping); }

  @Override
  public List<RouteDefinition> allRoutes() {
    List<RouteDefinition> result = new ArrayList<>();
    for (Entry entry : entries) result.add(entry.definition);
    return Collections.unmodifiableList(result);
  }

  /** 编译模板：支持 {name}、{name:regex}，以及段尾部的 '?'（仅允许末尾连续可选段） */
  private Route compileTemplate(String template, HttpRequestHandler handler) {
    String[] tSegs = fastSplit(template);
    int n = tSegs.length;

    String[] segLiterals = new String[n];
    String[] varNames = new String[n];
    Pattern[] varPatterns = new Pattern[n];
    boolean[] optional = new boolean[n];

    String firstLiteral = null;
    int firstLiteralIndex = -1;

    boolean seenOptional = false; // 一旦出现可选段，后续都必须可选（尾部连续可选）
    for (int i = 0; i < n; i++) {
      String raw = tSegs[i];
      boolean opt = false;

      // 段级可选语法：{...}? —— “?” 在整段的最后（不在大括号内）
      if (raw.endsWith("?")) {
        opt = true;
        raw = raw.substring(0, raw.length() - 1);
      }

      if (opt) {
        seenOptional = true;
      } else if (seenOptional) {
        // 可选段之后又出现必需段 —— 不允许（限制为末尾连续可选，简洁高效）
        throw new IllegalArgumentException("Optional segments must be trailing: " + template);
      }

      optional[i] = opt;

      if (isVarSegment(raw)) {
        // 去掉 {}
        String inside = raw.substring(1, raw.length() - 1).trim();
        int colon = inside.indexOf(':');
        String name, expr = null;
        if (colon >= 0) {
          name = inside.substring(0, colon).trim();
          expr = inside.substring(colon + 1).trim();
        } else {
          name = inside;
        }
        if (name.isEmpty()) {
          throw new IllegalArgumentException("Empty variable in route: " + template);
        }
        varNames[i] = name;
        if (expr != null && !expr.isEmpty()) {
          varPatterns[i] = Pattern.compile(expr);
        }
      } else {
        segLiterals[i] = raw;
        if (firstLiteral == null) {
          firstLiteral = raw;
          firstLiteralIndex = i;
        }
      }
    }

    int required = n;
    // 从尾部开始统计可选段数量
    for (int i = n - 1; i >= 0; i--) {
      if (optional[i]) {
        required--;
      } else {
        break;
      }
    }

    return new Route(template, handler, segLiterals, varNames, varPatterns, optional, n, required, firstLiteral,
        firstLiteralIndex);
  }

  /** 分段匹配（含可选段）并注入变量 */
  private boolean matchesTemplate(Route r, String[] segs, Map<String, String> params) {
    final int m = segs.length;

    // 段数范围：必须在 [requiredSegments, segmentsCount] 之间
    if (m < r.requiredSegments || m > r.segmentsCount)
      return false;

    // 若首静态段在 index 0，做快速预检
    if (r.firstLiteralIndex == 0 && r.firstLiteral != null) {
      if (m == 0 || !r.firstLiteral.equals(segs[0]))
        return false;
    }

    // 逐段匹配：仅匹配存在的前 m 段；缺失的尾部段必须是可选段
    for (int i = 0; i < r.segmentsCount; i++) {
      if (i >= m) {
        // 请求缺段：必须模板段是可选
        if (!r.optionalMask[i])
          return false;
        continue;
      }
      String lit = r.segLiterals[i];
      if (lit != null) {
        if (!lit.equals(segs[i]))
          return false;
      } else {
        Pattern p = r.varPatterns[i];
        if (p != null && !p.matcher(segs[i]).matches())
          return false;
      }
    }

    // 匹配成功：注入已出现的变量段
    for (int i = 0; i < m; i++) {
      String name = r.varNames[i];
      if (name != null) {
        params.put(name, segs[i]);
      }
    }
    return true;
  }

  /** 是否是 {xxx} 段（不含末尾的段级 ?） */
  private static boolean isVarSegment(String seg) {
    return seg.length() >= 2 && seg.charAt(0) == '{' && seg.charAt(seg.length() - 1) == '}';
  }

  /** 快速分割路径/模板（不使用正则），去掉开头的 '/'；保留空段过滤（不会产生空段） */
  private static String[] fastSplit(String path) {
    if (path == null || path.isEmpty())
      return new String[0];
    int start = (path.charAt(0) == '/') ? 1 : 0;

    // 统计 '/' 数量
    int count = 0;
    for (int i = start; i < path.length(); i++) {
      if (path.charAt(i) == '/')
        count++;
    }

    int partsCap = (path.length() > start) ? (count + 1) : 0;
    if (partsCap == 0)
      return new String[0];

    List<String> parts = new ArrayList<>(partsCap);
    int segStart = start;
    for (int i = start; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        if (i > segStart)
          parts.add(path.substring(segStart, i));
        segStart = i + 1;
      }
    }
    if (segStart < path.length()) {
      parts.add(path.substring(segStart));
    }
    return parts.toArray(new String[0]);
  }

  private static String keyOf(int segments, String firstLiteral) {
    return segments + "|" + firstLiteral;
  }
}