package com.litongjava.tio.boot.http.handler;

import java.util.concurrent.atomic.AtomicInteger;

import com.litongjava.tio.boot.constatns.TioBootConfigKeys;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.session.limiter.SessionRateLimiter;
import com.litongjava.tio.http.common.session.limiter.SessionRateVo;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.environment.EnvUtils;

public class TioServerSessionRateLimiter implements SessionRateLimiter {

  private static final int MAX_REQUESTS_PER_SECOND = EnvUtils.getInt(TioBootConfigKeys.HTTP_MAX_REQUESTS_PER_SECOND,
      10);

  @Override
  public boolean allow(HttpRequest request, SessionRateVo sessionRateVo) {
    AtomicInteger accessCount = sessionRateVo.getAccessCount();
    long currentTime = System.currentTimeMillis();
    long lastAccessTime = sessionRateVo.getLastAccessTime();

    if (currentTime - lastAccessTime < 1000) {
      return accessCount.get() < MAX_REQUESTS_PER_SECOND;
    } else {
      accessCount.set(0);
      return true;
    }
  }

  @Override
  public HttpResponse response(HttpRequest request, SessionRateVo sessionRateVo) {
    HttpResponse httpResponse = Resps.txt(request, "Too Many Requests");
    httpResponse.setStatus(429);
    return httpResponse;
  }
}
