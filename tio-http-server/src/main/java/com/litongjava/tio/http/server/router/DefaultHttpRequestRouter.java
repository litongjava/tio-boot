package com.litongjava.tio.http.server.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

public class DefaultHttpRequestRouter implements HttpRequestRouter {
  /** 精确/通配符路由 */
  private final Map<String, HttpRequestHandler> requestMapping = new ConcurrentHashMap<>();

  /** 模板路由快照（读多写少） */
  private final CopyOnWriteArrayList<Route> templateRoutes = new CopyOnWriteArrayList<>();

  /** 段数 -> 路由列表（读路径无锁） */
  private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<Route>> routesBySegments = new ConcurrentHashMap<>();

  /** (段数|首静态段) -> 路由列表（更窄的候选集，仅当首静态段在下标0时建立） */
  private final ConcurrentHashMap<String, CopyOnWriteArrayList<Route>> routesByKey = new ConcurrentHashMap<>();

  /** 路由命中缓存（仅缓存路由选择，不缓存参数值） */
  private final ConcurrentHashMap<String, Route> routeHitCache = new ConcurrentHashMap<>(1024);

  @Override
  public void add(String path, HttpRequestHandler handler) {
    if (path.contains("{") && path.contains("}")) {
      Route r = compileTemplate(path, handler);
      templateRoutes.add(r);

      // 段数索引
      routesBySegments.computeIfAbsent(r.segmentsCount, k -> new CopyOnWriteArrayList<>()).add(r);

      // 仅当首静态段在下标0时，建立 (段数|首静态段) 索引
      if (r.firstLiteralIndex == 0 && r.firstLiteral != null) {
        String key = keyOf(r.segmentsCount, r.firstLiteral);
        routesByKey.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(r);
      }
    } else {
      requestMapping.put(path, handler);
    }
  }

  @Override
  public HttpRequestHandler find(String path) {
    HttpRequestHandler direct = requestMapping.get(path);
    if (direct != null)
      return direct;

    for (Map.Entry<String, HttpRequestHandler> entry : requestMapping.entrySet()) {
      String key = entry.getKey();
      if (key.endsWith("/*")) {
        String base = key.substring(0, key.length() - 1);
        if (path.startsWith(base))
          return entry.getValue();
      } else if (key.endsWith("/**")) {
        String base = key.substring(0, key.length() - 2);
        if (path.startsWith(base))
          return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public HttpRequestHandler resolve(HttpRequest request) {
    final String path = request.getRequestURI();

    // 1) 先走精确/通配符
    HttpRequestHandler h = find(path);
    if (h != null)
      return h;

    // 2) 命中缓存（再次校验并注入，避免脏缓存）
    Route cached = routeHitCache.get(path);
    if (cached != null) {
      String[] segs = fastSplit(path);
      if (matchAndInject(cached, segs, request)) {
        return cached.handler;
      }
    }

    // 3) 计算候选集
    String[] segs = fastSplit(path);
    int n = segs.length;

    // 优先用 (段数|首静态段=segs[0]) 的索引（仅适用于首静态段在0位的模板）
    List<Route> candidates = null;
    if (n > 0) {
      candidates = routesByKey.get(keyOf(n, segs[0]));
    }
    if (candidates == null) {
      // 退回到相同 segmentsCount 的模板集合
      candidates = routesBySegments.get(n);
    }
    if (candidates == null) {
      // 极少走到：最后全表扫描模板
      candidates = templateRoutes;
    }

    // 4) 分段匹配（常量比较 + 少数小正则 + 可选段）
    for (Route r : candidates) {
      if (matchAndInject(r, segs, request)) {
        routeHitCache.put(path, r); // 仅缓存路由选择
        return r.handler;
      }
    }
    return null;
  }

  @Override
  public Map<String, HttpRequestHandler> all() {
    return requestMapping;
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
  private boolean matchAndInject(Route r, String[] segs, HttpRequest req) {
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
        req.addParam(name, segs[i]);
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