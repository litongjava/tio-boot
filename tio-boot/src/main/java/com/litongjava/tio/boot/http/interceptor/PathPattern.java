package com.litongjava.tio.boot.http.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** 轻量路径模式匹配：支持 exact, /*, /**, 以及 {var}、{var:regex}、段级 ?(仅末尾连续可选) */
public final class PathPattern {
  private static final ConcurrentHashMap<String, PathPattern> CACHE = new ConcurrentHashMap<>();

  public static PathPattern compile(String pattern) {
    return CACHE.computeIfAbsent(pattern, PathPattern::new);
  }

  private final String raw;

  // 三类快速路径
  private final boolean isDoubleStar; // /** 前缀匹配
  private final boolean isSingleStar; // /* 前缀匹配
  private final String starBase; // 对应上面两种的基底

  // 模板匹配
  private final String[] segLiterals; // 非变量段字面；变量段为 null
  private final String[] varNames; // 变量名；非变量段为 null
  private final Pattern[] varPatterns; // 变量段正则；无约束为 null
  private final boolean[] optionalMask; // 段是否可选（仅允许末尾连续）
  private final int segmentsCount;
  private final int requiredSegments;

  private PathPattern(String raw) {
    this.raw = raw;

    // 1) /** 与 /* 快速路径
    if (raw.endsWith("/**")) {
      this.isDoubleStar = true;
      this.isSingleStar = false;
      this.starBase = raw.substring(0, raw.length() - 3);
      this.segLiterals = null;
      this.varNames = null;
      this.varPatterns = null;
      this.optionalMask = null;
      this.segmentsCount = 0;
      this.requiredSegments = 0;
      return;
    } else if (raw.endsWith("/*")) {
      this.isDoubleStar = false;
      this.isSingleStar = true;
      this.starBase = raw.substring(0, raw.length() - 2);
      this.segLiterals = null;
      this.varNames = null;
      this.varPatterns = null;
      this.optionalMask = null;
      this.segmentsCount = 0;
      this.requiredSegments = 0;
      return;
    } else {
      this.isDoubleStar = false;
      this.isSingleStar = false;
      this.starBase = null;
    }

    // 2) 纯等值（不含 { } ? 且不含星号）
    boolean hasTemplateChars = raw.indexOf('{') >= 0 || raw.indexOf('}') >= 0 || raw.indexOf('?') >= 0;
    boolean hasStar = raw.indexOf('*') >= 0;
    if (!hasTemplateChars && !hasStar) {
      // 当作模板的 degenerate 情况（无变量无可选）以复用分段逻辑
    }

    // 3) 编译为“分段模板”
    String[] tSegs = fastSplit(raw);
    int n = tSegs.length;

    String[] segLiterals = new String[n];
    String[] varNames = new String[n];
    Pattern[] varPatterns = new Pattern[n];
    boolean[] optional = new boolean[n];

    boolean seenOptional = false;
    for (int i = 0; i < n; i++) {
      String segRaw = tSegs[i];
      boolean opt = false;

      if (segRaw.endsWith("?")) {
        opt = true;
        segRaw = segRaw.substring(0, segRaw.length() - 1);
      }
      if (opt) {
        seenOptional = true;
      } else if (seenOptional) {
        throw new IllegalArgumentException("Optional segments must be trailing: " + raw);
      }
      optional[i] = opt;

      if (isVarSegment(segRaw)) {
        String inside = segRaw.substring(1, segRaw.length() - 1).trim();
        int colon = inside.indexOf(':');
        String name, expr = null;
        if (colon >= 0) {
          name = inside.substring(0, colon).trim();
          expr = inside.substring(colon + 1).trim();
        } else {
          name = inside;
        }
        if (name.isEmpty()) {
          throw new IllegalArgumentException("Empty variable in pattern: " + raw);
        }
        varNames[i] = name;
        if (expr != null && !expr.isEmpty()) {
          varPatterns[i] = Pattern.compile(expr);
        }
      } else {
        segLiterals[i] = segRaw;
      }
    }

    int required = n;
    for (int i = n - 1; i >= 0; i--) {
      if (optional[i])
        required--;
      else
        break;
    }

    this.segLiterals = segLiterals;
    this.varNames = varNames;
    this.varPatterns = varPatterns;
    this.optionalMask = optional;
    this.segmentsCount = n;
    this.requiredSegments = required;
  }

  public boolean matches(String path) {
    if (isDoubleStar) {
      return path.startsWith(starBase);
    }
    if (isSingleStar) {
      return path.startsWith(starBase);
    }
    // 精确相等（无模板符号的字符串）
    if (segLiterals == null) {
      return raw.equals(path);
    }

    String[] segs = fastSplit(path);
    int m = segs.length;
    if (m < requiredSegments || m > segmentsCount)
      return false;

    for (int i = 0; i < segmentsCount; i++) {
      if (i >= m) {
        if (!optionalMask[i])
          return false;
        continue;
      }
      String lit = segLiterals[i];
      if (lit != null) {
        if (!lit.equals(segs[i]))
          return false;
      } else {
        Pattern p = varPatterns[i];
        if (p != null && !p.matcher(segs[i]).matches())
          return false;
      }
    }
    return true;
  }

  private static boolean isVarSegment(String seg) {
    return seg.length() >= 2 && seg.charAt(0) == '{' && seg.charAt(seg.length() - 1) == '}';
  }

  private static String[] fastSplit(String path) {
    if (path == null || path.isEmpty())
      return new String[0];
    int start = (path.charAt(0) == '/') ? 1 : 0;

    int count = 0;
    for (int i = start; i < path.length(); i++)
      if (path.charAt(i) == '/')
        count++;

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
}
