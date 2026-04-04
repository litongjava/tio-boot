package com.litongjava.tio.boot.http.handler.internal;

import java.util.concurrent.atomic.AtomicInteger;

import com.litongjava.tio.utils.environment.EnvUtils;

import nexus.io.constants.ServerConfigKeys;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.session.limiter.SessionRateLimiter;
import nexus.io.tio.http.common.session.limiter.SessionRateVo;
import nexus.io.tio.http.server.util.Resps;

public class TioServerSessionRateLimiter implements SessionRateLimiter {

  private static final int MAX_REQUESTS_PER_SECOND = EnvUtils.getInt(ServerConfigKeys.HTTP_MAX_REQUESTS_PER_SECOND, 10);

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
