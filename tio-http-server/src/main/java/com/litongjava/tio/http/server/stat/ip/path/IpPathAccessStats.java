package com.litongjava.tio.http.server.stat.ip.path;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.TioConfig;
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
public class IpPathAccessStats {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(IpPathAccessStats.class);

  private final static String CACHE_NAME = "TIO_IP_ACCESSPATH";
  // private final static Long timeToLiveSeconds = null;
  // private final static Long timeToIdleSeconds = Time.DAY_1;

  private TioConfig tioConfig;

  private String tioConfigId;

  private StatPathFilter statPathFilter;

  // private CaffeineCache[] caches = null;
  /**
   * key:   时长段，单位：秒
   * value: CaffeineCache: key: ip, value: IpAccessStat
   */
  public final Map<Long, AbsCache> cacheMap = new HashMap<>();

  /**
   * 时长段列表
   */
  public final List<Long> durationList = new ArrayList<>();

  private final Map<Long, IpPathAccessStatListener> listenerMap = new HashMap<>();

  /**
   * 
   * @param tioConfig
   * @param ipPathAccessStatListener
   * @param durations
   * @author tanyaowu
   */
  public IpPathAccessStats(StatPathFilter statPathFilter, TioConfig tioConfig,
      IpPathAccessStatListener ipPathAccessStatListener, Long[] durations,RemovalListenerWrapper<?> removalListenerWrapper) {
    this.statPathFilter = statPathFilter;
    if (this.statPathFilter == null) {
      this.statPathFilter = DefaultStatPathFilter.me;
    }
    this.tioConfig = tioConfig;
    this.tioConfigId = tioConfig.getId();
    if (durations != null) {
      for (Long duration : durations) {
        addDuration(duration, ipPathAccessStatListener,removalListenerWrapper);
      }
    }
  }

  /**
   * 添加监控时段
   * @param duration 单位：秒
   * @param ipPathAccessStatListener 可以为null
   * @author: tanyaowu
   */
  public void addDuration(Long duration, IpPathAccessStatListener ipPathAccessStatListener,RemovalListenerWrapper<?> removalListenerWrapper) {
    CacheFactory cacheFactory = tioConfig.getCacheFactory();
    //IpPathAccessStatRemovalListener ipPathAccessStatRemovalListener = new IpPathAccessStatRemovalListener(tioConfig, ipPathAccessStatListener);
    
    AbsCache absCache = cacheFactory.register(getCacheName(duration), duration, null,removalListenerWrapper);
    cacheMap.put(duration,absCache);
    durationList.add(duration);

    if (ipPathAccessStatListener != null) {
      listenerMap.put(duration, ipPathAccessStatListener);
    }
  }

  /**
   * 
   * @param duration
   * @return
   * @author tanyaowu
   */
  public IpPathAccessStatListener getListener(Long duration) {
    return listenerMap.get(duration);
  }

  /**
   * 添加监控时段
   * @param durations 单位：秒
   * @param ipPathAccessStatListener 可以为null
   * @author: tanyaowu
   */
  public void addDurations(Long[] durations, IpPathAccessStatListener ipPathAccessStatListener,RemovalListenerWrapper<?> removalListenerWrapper) {
    if (durations != null) {
      for (Long duration : durations) {
        addDuration(duration, ipPathAccessStatListener,removalListenerWrapper);
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
   * 获取IpAccessStat
   * @param duration
   * @param ip
   * @param forceCreate
   * @return
   * @author tanyaowu
   */
  public IpAccessStat get(Long duration, String ip, boolean forceCreate) {
    if (StrUtil.isBlank(ip)) {
      return null;
    }

    AbsCache caffeineCache = cacheMap.get(duration);
    if (caffeineCache == null) {
      return null;
    }

    IpAccessStat ipAccessStat = (IpAccessStat) caffeineCache.get(ip);
    if (ipAccessStat == null && forceCreate) {
      synchronized (caffeineCache) {
        ipAccessStat = (IpAccessStat) caffeineCache.get(ip);
        if (ipAccessStat == null) {
          ipAccessStat = new IpAccessStat(duration, ip);// new MapWithLock<String, IpPathAccessStat>(new HashMap<>());//new IpPathAccessStat(duration, ip, path);
          caffeineCache.put(ip, ipAccessStat);
        }
      }
    }

    return ipAccessStat;
  }

  /**
   * 获取IpAccessStat
   * @param duration
   * @param ip
   * @return
   * @author tanyaowu
   */
  public IpAccessStat get(Long duration, String ip) {
    return get(duration, ip, true);
  }

  /**
   * key:   ip
   * value: IpPathAccessStat
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

  public StatPathFilter getStatPathFilter() {
    return statPathFilter;
  }

  public void setStatPathFilter(StatPathFilter statPathFilter) {
    this.statPathFilter = statPathFilter;
  }
}
