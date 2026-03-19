package com.litongjava.tio.http.server.stat.token;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.http.server.intf.CurrUseridGetter;
import com.litongjava.tio.http.server.stat.DefaultStatPathFilter;
import com.litongjava.tio.http.server.stat.StatPathFilter;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * 
 * @author tanyaowu
 * 2017年4月15日 下午12:13:19
 */
public class TokenPathAccessStats {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(TokenPathAccessStats.class);

  private final static String CACHE_NAME = "TIO_TOKEN_ACCESSPATH";
  // private final static Long timeToLiveSeconds = null;
  // private final static Long timeToIdleSeconds = Time.DAY_1;

  private TioConfig tioConfig;

  private String tioConfigId;

  private StatPathFilter statPathFilter;

  // private CaffeineCache[] caches = null;
  /**
   * key:   时长段，单位：秒
   * value: CaffeineCache: key: token, value: TokenAccessStat
   */
  public final Map<Long, AbsCache> cacheMap = new HashMap<>();

  /**
   * 时长段列表
   */
  public final List<Long> durationList = new ArrayList<>();

  private final Map<Long, TokenPathAccessStatListener> listenerMap = new HashMap<>();

  private TokenGetter tokenGetter;

  private CurrUseridGetter currUseridGetter;

  /**
   * 
   * @param statPathFilter
   * @param tokenGetter
   * @param currUseridGetter
   * @param tioConfig
   * @param tokenPathAccessStatListener
   * @param durations
   */
  public TokenPathAccessStats(StatPathFilter statPathFilter, TokenGetter tokenGetter, CurrUseridGetter currUseridGetter,
      TioConfig tioConfig, TokenPathAccessStatListener tokenPathAccessStatListener, Long[] durations,RemovalListenerWrapper<?> removalListenerWrapper) {
    this.statPathFilter = statPathFilter;
    if (this.statPathFilter == null) {
      this.statPathFilter = DefaultStatPathFilter.me;
    }

    if (tokenGetter == null) {
      throw new RuntimeException("tokenGetter can not be null");
    }

    this.tokenGetter = tokenGetter;
    this.currUseridGetter = currUseridGetter;
    this.tioConfig = tioConfig;
    this.tioConfigId = tioConfig.getId();
    if (durations != null) {
      for (Long duration : durations) {
        addDuration(duration, tokenPathAccessStatListener,removalListenerWrapper);
      }
    }
  }

  public TokenPathAccessStats(StatPathFilter statPathFilter, CurrUseridGetter currUseridGetter, TioConfig tioConfig,
      TokenPathAccessStatListener tokenPathAccessStatListener, Long[] durations,RemovalListenerWrapper<?> removalListenerWrapper) {
    this(statPathFilter, DefaultTokenGetter.me, currUseridGetter, tioConfig, tokenPathAccessStatListener, durations,removalListenerWrapper);
  }

  /**
   * 添加监控时段
   * @param duration 单位：秒
   * @param tokenPathAccessStatListener 可以为null
   * @author: tanyaowu
   */
  public void addDuration(Long duration, TokenPathAccessStatListener tokenPathAccessStatListener,RemovalListenerWrapper<?> removalListenerWrapper) {
    //new TokenPathAccessStatRemovalListener(tioConfig, tokenPathAccessStatListener)
    CacheFactory cacheFactory = tioConfig.getCacheFactory();
    AbsCache absCache = cacheFactory.register(getCacheName(duration), duration, null,removalListenerWrapper);
        
    cacheMap.put(duration, absCache);
    durationList.add(duration);

    if (tokenPathAccessStatListener != null) {
      listenerMap.put(duration, tokenPathAccessStatListener);
    }
  }

  /**
   * 
   * @param duration
   * @return
   * @author tanyaowu
   */
  public TokenPathAccessStatListener getListener(Long duration) {
    return listenerMap.get(duration);
  }

  /**
   * 添加监控时段
   * @param durations 单位：秒
   * @param tokenPathAccessStatListener 可以为null
   * @author: tanyaowu
   */
  public void addDurations(Long[] durations, TokenPathAccessStatListener tokenPathAccessStatListener,RemovalListenerWrapper<?> removalListenerWrapper) {
    if (durations != null) {
      for (Long duration : durations) {
        addDuration(duration, tokenPathAccessStatListener,removalListenerWrapper);
      }
    }
  }

  /**
   * 删除监控时间段
   * @param duration
   * @author: tanyaowu
   */
  public void removeMonitor(Long duration) {
    clear(duration);
    cacheMap.remove(duration);
    durationList.remove(duration);
  }

  /**
   * 
   * @param duration
   * @return
   * @author: tanyaowu
   */
  public String getCacheName(Long duration) {
    String cacheName = CACHE_NAME + "_" + this.tioConfigId + "_";
    return cacheName + duration;
  }

  /**
   * 清空监控数据
   * @author: tanyaowu
   */
  public void clear(Long duration) {
    AbsCache caffeineCache = cacheMap.get(duration);
    if (caffeineCache == null) {
      return;
    }
    caffeineCache.clear();
  }

  /**
   * 获取TokenAccessStat
   * @param duration
   * @param token
   * @param ip
   * @param uid
   * @param forceCreate
   * @return
   */
  public TokenAccessStat get(Long duration, String token, String ip, String uid, boolean forceCreate) {
    if (StrUtil.isBlank(token)) {
      return null;
    }

    AbsCache caffeineCache = cacheMap.get(duration);
    if (caffeineCache == null) {
      return null;
    }

    TokenAccessStat tokenAccessStat = (TokenAccessStat) caffeineCache.get(token);
    if (tokenAccessStat == null && forceCreate) {
      synchronized (caffeineCache) {
        tokenAccessStat = (TokenAccessStat) caffeineCache.get(token);
        if (tokenAccessStat == null) {
          tokenAccessStat = new TokenAccessStat(duration, token, ip, uid);// new MapWithLock<String, TokenPathAccessStat>(new HashMap<>());//new TokenPathAccessStat(duration, token, path);
          caffeineCache.put(token, tokenAccessStat);
        }
      }
    }

    return tokenAccessStat;
  }

  /**
   * 获取TokenAccessStat
   * @param duration
   * @param token
   * @param ip
   * @param uid
   * @return
   */
  public TokenAccessStat get(Long duration, String token, String ip, String uid) {
    return get(duration, token, ip, uid, true);
  }

  /**
   * key:   token
   * value: TokenPathAccessStat
   * @param duration
   * @return
   * @author tanyaowu
   */
  public Map<String, Serializable> map(Long duration) {
    AbsCache caffeineCache = cacheMap.get(duration);
    if (caffeineCache == null) {
      return null;
    }
    Map<String, Serializable> map = caffeineCache.asMap();
    return map;
  }

  /**
   *
   * @return
   * @author: tanyaowu
   */
  public Long size(Long duration) {
    AbsCache caffeineCache = cacheMap.get(duration);
    if (caffeineCache == null) {
      return null;
    }
    return caffeineCache.size();
  }

  /**
   *
   * @return
   * @author: tanyaowu
   */
  public Collection<Serializable> values(Long duration) {
    AbsCache caffeineCache = cacheMap.get(duration);
    if (caffeineCache == null) {
      return null;
    }
    Collection<Serializable> set = caffeineCache.asMap().values();
    return set;
  }

  public TokenGetter getTokenGetter() {
    return tokenGetter;
  }

  public CurrUseridGetter getCurrUseridGetter() {
    return currUseridGetter;
  }

  public void setCurrUseridGetter(CurrUseridGetter currUseridGetter) {
    this.currUseridGetter = currUseridGetter;
  }

  public StatPathFilter getStatPathFilter() {
    return statPathFilter;
  }

  public void setStatPathFilter(StatPathFilter statPathFilter) {
    this.statPathFilter = statPathFilter;
  }

  // public void setTokenGetter(TokenGetter tokenGetter) {
  // this.tokenGetter = tokenGetter;
  // }

}
