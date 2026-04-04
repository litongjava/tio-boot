package com.litongjava.tio.boot.http.handler.internal;

import java.util.List;

import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.hutool.StrUtil;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.intf.CurrUseridGetter;
import nexus.io.tio.http.server.session.HttpSessionUtils;
import nexus.io.tio.http.server.stat.StatPathFilter;
import nexus.io.tio.http.server.stat.ip.path.IpAccessStat;
import nexus.io.tio.http.server.stat.ip.path.IpPathAccessStat;
import nexus.io.tio.http.server.stat.ip.path.IpPathAccessStatListener;
import nexus.io.tio.http.server.stat.ip.path.IpPathAccessStats;
import nexus.io.tio.http.server.stat.token.TokenAccessStat;
import nexus.io.tio.http.server.stat.token.TokenPathAccessStat;
import nexus.io.tio.http.server.stat.token.TokenPathAccessStatListener;
import nexus.io.tio.http.server.stat.token.TokenPathAccessStats;

public class AccessStatisticsHandler {

  /**
   * ipPathAccessStat and ipAccessStat
   *
   * @param request
   * @param response
   * @param path
   * @param iv
   * @return
   */
  public boolean statIpPath(IpPathAccessStats ipPathAccessStats, HttpRequest request, HttpResponse response,
      String path, long iv) {

    if (response == null) {
      return false;
    }

    if (response.isSkipIpStat() || request.isClosed()) {
      return true;
    }

    // 统计一下IP访问数据
    String ip = request.getClientIp();// IpUtils.getRealIp(request);

    // Cookie cookie = getSessionCookie(request, httpConfig);
    String sessionId = HttpSessionUtils.getSessionId(request);

    StatPathFilter statPathFilter = ipPathAccessStats.getStatPathFilter();

    // 添加统计
    for (Long duration : ipPathAccessStats.durationList) {
      IpAccessStat ipAccessStat = ipPathAccessStats.get(duration, ip);// .get(duration, ip, path);//.get(v, channelContext.getClientNode().getIp());

      ipAccessStat.count.incrementAndGet();
      ipAccessStat.timeCost.addAndGet(iv);
      ipAccessStat.setLastAccessTime(SystemTimer.currTime);
      if (StrUtil.isBlank(sessionId)) {
        ipAccessStat.noSessionCount.incrementAndGet();
      } else {
        ipAccessStat.sessionIds.add(sessionId);
      }

      if (statPathFilter.filter(path, request, response)) {
        IpPathAccessStat ipPathAccessStat = ipAccessStat.get(path);
        ipPathAccessStat.count.incrementAndGet();
        ipPathAccessStat.timeCost.addAndGet(iv);
        ipPathAccessStat.setLastAccessTime(SystemTimer.currTime);

        if (StrUtil.isBlank(sessionId)) {
          ipPathAccessStat.noSessionCount.incrementAndGet();
        }
        // else {
        // ipAccessStat.sessionIds.add(cookie.getValue());
        // }

        IpPathAccessStatListener ipPathAccessStatListener = ipPathAccessStats.getListener(duration);
        if (ipPathAccessStatListener != null) {
          boolean isContinue = ipPathAccessStatListener.onChanged(request, ip, path, ipAccessStat, ipPathAccessStat);
          if (!isContinue) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * tokenPathAccessStat
   * @param tokenPathAccessStats 
   *
   * @param request
   * @param response
   * @param path
   * @param iv
   * @return
   */
  public boolean statTokenPath(TokenPathAccessStats tokenPathAccessStats, HttpRequest request, HttpResponse response,
      String path, long iv) {

    if (response == null) {
      return false;
    }

    if (response.isSkipTokenStat() || request.isClosed()) {
      return true;
    }

    // 统计一下Token访问数据
    String token = tokenPathAccessStats.getTokenGetter().getToken(request);
    if (StrUtil.isNotBlank(token)) {
      List<Long> list = tokenPathAccessStats.durationList;

      CurrUseridGetter currUseridGetter = tokenPathAccessStats.getCurrUseridGetter();
      String uid = null;
      if (currUseridGetter != null) {
        uid = currUseridGetter.getUserid(request);
      }

      StatPathFilter statPathFilter = tokenPathAccessStats.getStatPathFilter();

      // 添加统计
      for (Long duration : list) {
        TokenAccessStat tokenAccessStat = tokenPathAccessStats.get(duration, token, request.getClientIp(), uid);// .get(duration, ip, path);//.get(v, channelContext.getClientNode().getIp());

        tokenAccessStat.count.incrementAndGet();
        tokenAccessStat.timeCost.addAndGet(iv);
        tokenAccessStat.setLastAccessTime(SystemTimer.currTime);

        if (statPathFilter.filter(path, request, response)) {
          TokenPathAccessStat tokenPathAccessStat = tokenAccessStat.get(path);
          tokenPathAccessStat.count.incrementAndGet();
          tokenPathAccessStat.timeCost.addAndGet(iv);
          tokenPathAccessStat.setLastAccessTime(SystemTimer.currTime);

          TokenPathAccessStatListener tokenPathAccessStatListener = tokenPathAccessStats.getListener(duration);
          if (tokenPathAccessStatListener != null) {
            boolean isContinue = tokenPathAccessStatListener.onChanged(request, token, path, tokenAccessStat,
                tokenPathAccessStat);
            if (!isContinue) {
              return false;
            }
          }
        }
      }
    }

    return true;
  }

}
