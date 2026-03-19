package com.litongjava.tio.boot.druid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.alibaba.druid.support.http.stat.WebAppStat;
import com.alibaba.druid.support.http.stat.WebAppStatManager;
import com.alibaba.druid.support.http.stat.WebRequestStat;
import com.alibaba.druid.support.http.stat.WebURIStat;
import com.alibaba.druid.support.profile.ProfileEntryKey;
import com.alibaba.druid.support.profile.ProfileEntryReqStat;
import com.alibaba.druid.support.profile.Profiler;
import com.alibaba.druid.util.PatternMatcher;
import com.alibaba.druid.util.ServletPathMatcher;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

/**
 * DruidWebStatInterceptor for Tio-Boot without session and StatFilterContext
 */
public class DruidWebStatInterceptor implements HttpRequestInterceptor {
  private final PatternMatcher pathMatcher = new ServletPathMatcher();
  private final Set<String> excludes;
  private final boolean profileEnable;
  private final WebAppStat webAppStat;

  /**
   * @param contextPath   应用上下文路径，可为空串
   * @param exclusionsCsv 排除的 URL 模式, 逗号分隔
   * @param profileEnable 是否开启 Profile 调用耗时
   */
  public DruidWebStatInterceptor(String contextPath, String exclusionsCsv, boolean profileEnable) {
    this.excludes = new HashSet<>(Arrays.asList(exclusionsCsv.split("\\s*,\\s*")));
    this.profileEnable = profileEnable;
    // 初始化并注册 WebAppStat
    this.webAppStat = new WebAppStat(contextPath, 0);
    WebAppStatManager.getInstance().addWebAppStatSet(webAppStat);
  }

  private boolean isExclusion(String uri) {
    if (uri == null) {
      return false;
    }
    for (String pattern : excludes) {
      if (pathMatcher.matches(pattern, uri)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine line, HttpResponse originalResponse) throws Exception {
    String uri = line.getPath();
    if (isExclusion(uri)) {
      return null;
    }
    long startNano = System.nanoTime();
    long startMillis = System.currentTimeMillis();
    WebRequestStat wrs = new WebRequestStat(startNano, startMillis);
    WebRequestStat.set(wrs);
    webAppStat.beforeInvoke();
    WebURIStat uriStat = webAppStat.getURIStat(uri, false);
    if (uriStat != null) {
      uriStat.beforeInvoke();
    }
    if (profileEnable) {
      Profiler.initLocal();
      Profiler.enter(uri, Profiler.PROFILE_TYPE_WEB);
    }
    // 存储以便后置使用
    request.setLocalAttribute("_druid_startNano", startNano);
    request.setLocalAttribute("_druid_uriStat", uriStat);
    return null;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine line, HttpResponse response, long costMillis) throws Exception {
    String uri = line.getPath();
    if (isExclusion(uri)) {
      return;
    }
    Long startNano = (Long) request.getLocalAttribute("_druid_startNano");
    if (startNano == null) {
      return;
    }
    long endNano = System.nanoTime();
    long nanos = endNano - startNano;
    WebRequestStat wrs = WebRequestStat.current();
    wrs.setEndNano(endNano);
    webAppStat.afterInvoke(null, nanos);
    WebURIStat uriStat = (WebURIStat) request.getLocalAttribute("_druid_uriStat");
    if (uriStat == null) {
      uriStat = webAppStat.getURIStat(uri, true);
      if (uriStat != null) {
        uriStat.beforeInvoke();
      }
    }
    if (uriStat != null) {
      uriStat.afterInvoke(null, nanos);
      if (profileEnable) {
        Profiler.release(nanos);
        Map<ProfileEntryKey, ProfileEntryReqStat> stats = Profiler.getStatsMap();
        uriStat.getProfiletat().record(stats);
        Profiler.removeLocal();
      }
    }
    WebRequestStat.set(null);
  }
}
