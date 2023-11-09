package com.litongjava.tio.boot.handler;

import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.session.HttpSession;
import org.tio.http.common.session.limiter.SessionRateLimiter;
import org.tio.http.common.session.limiter.SessionRateVo;
import org.tio.utils.SystemTimer;
import org.tio.utils.cache.caffeine.CaffeineCache;


public class SessionLimit {
  private boolean myResult;
  private HttpRequest request;
  private HttpResponse response;
  private String path;

  public SessionLimit(HttpRequest request, HttpResponse response, String path) {
    this.request = request;
    this.response = response;
    this.path = path;
  }

  boolean is() {
    return myResult;
  }

  public HttpResponse getResponse() {
    return response;
  }

  public SessionLimit invoke(HttpConfig httpConfig, CaffeineCache sessionRateLimiterCache, String SESSIONRATELIMITER_KEY_SPLIT) {
    SessionRateLimiter sessionRateLimiter = httpConfig.sessionRateLimiter;
    if (sessionRateLimiter != null) {
      boolean pass = false;

      HttpSession httpSession = request.getHttpSession();
      String key = path + SESSIONRATELIMITER_KEY_SPLIT + httpSession.getId();
      SessionRateVo sessionRateVo = sessionRateLimiterCache.get(key, SessionRateVo.class);
      if (sessionRateVo == null) {
        synchronized (httpSession) {
          sessionRateVo = sessionRateLimiterCache.get(key, SessionRateVo.class);
          if (sessionRateVo == null) {
            sessionRateVo = SessionRateVo.create(path);
            sessionRateLimiterCache.put(key, sessionRateVo);
            pass = true;
          }
        }
      }

      if (!pass) {
        if (sessionRateLimiter.allow(request, sessionRateVo)) {
          pass = true;
        }
      }

      if (!pass) {
        response = sessionRateLimiter.response(request, sessionRateVo);
        myResult = true;
        return this;
      }

      // 更新上次访问时间（放在这个位置：被拒绝访问的就不更新lastAccessTime）
      sessionRateVo.setLastAccessTime(SystemTimer.currTime);
      sessionRateVo.getAccessCount().incrementAndGet();
    }
    myResult = false;
    return this;
  }
}