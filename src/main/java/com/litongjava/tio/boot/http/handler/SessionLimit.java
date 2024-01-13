package com.litongjava.tio.boot.http.handler;

import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.common.session.limiter.SessionRateLimiter;
import com.litongjava.tio.http.common.session.limiter.SessionRateVo;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.cache.AbsCache;

public class SessionLimit {

  /**
   * 判断是否进行限流
   * @param request
   * @param path
   * @param httpConfig
   * @param sessionRateLimiterCache
   * @return
   */
  public static HttpResponse build(HttpRequest request, String path, HttpConfig httpConfig,
      AbsCache sessionRateLimiterCache) {
    SessionRateLimiter sessionRateLimiter = httpConfig.sessionRateLimiter;
    if (sessionRateLimiter != null) {

      HttpSession httpSession = request.getHttpSession();
      String key = path + DefaultHttpRequestConstants.SESSION_RATE_LIMITER_KEY_SPLIT + httpSession.getId();
      SessionRateVo sessionRateVo = sessionRateLimiterCache.get(key, SessionRateVo.class);
      if (sessionRateVo == null) {
        synchronized (httpSession) {
          // 第一次访问发访问,自动创建sessionRateVo并添加了sessionRateLimiterCache
          sessionRateVo = sessionRateLimiterCache.get(key, SessionRateVo.class);
          if (sessionRateVo == null) {
            sessionRateVo = SessionRateVo.create(path);
            sessionRateLimiterCache.put(key, sessionRateVo);
            return null;
          }
        }
      }

      HttpResponse response = null;
      if (!sessionRateLimiter.allow(request, sessionRateVo)) {
        response = sessionRateLimiter.response(request, sessionRateVo);
      }

      // 更新上次访问时间（放在这个位置：被拒绝访问的就不更新lastAccessTime）
      sessionRateVo.setLastAccessTime(SystemTimer.currTime);
      sessionRateVo.getAccessCount().incrementAndGet();
      return response;
    } else {
      return null;
    }
  }
}