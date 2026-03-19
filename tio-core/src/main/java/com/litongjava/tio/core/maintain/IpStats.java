package com.litongjava.tio.core.maintain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.RemovalListenerWrapper;
import com.litongjava.tio.utils.hutool.CollUtil;

/**
 * 使用方法（注意顺序）：<br>
 * 1、serverTioConfig.setIpStatListener(ShowcaseIpStatListener.me);
 * 2、serverTioConfig.ipStats.addDuration(Time.MINUTE_1 * 5);
 * @author tanyaowu
 * 2017年4月15日 下午12:13:19
 */
public class IpStats {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(IpStats.class);

  private final static String CACHE_NAME = "TIO_IP_STAT";

  private String tioConfigId;
  private TioConfig tioConfig;

  /**
   * key: 时长，单位：秒
   */
  public final Map<Long, AbsCache> cacheMap = new HashMap<>();

  public List<Long> durationList = null;// new ArrayList<>();

  public IpStats(TioConfig tioConfig, Long[] durations) {
    this.tioConfig = tioConfig;
    this.tioConfigId = tioConfig.getId();
    if (durations != null) {
      addDurations(durations);
    }
  }

  /**
   * 添加监控时段，不要添加过多的时间段，因为每个时间段都要消耗一份内存，一般加一个时间段就可以了
   * @param duration 单位：秒
   * @author: tanyaowu
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void addDuration(Long duration) {
    synchronized (this) {
      if (durationList == null) {
        durationList = new ArrayList<>();
      }
      RemovalListenerWrapper removalListenerWrapper=tioConfig.getIpRemovalListenerWrapper();
      AbsCache caffeineCache = tioConfig.getCacheFactory().register(getCacheName(duration), duration, null,
          removalListenerWrapper);
      cacheMap.put(duration, caffeineCache);
      durationList.add(duration);
    }
  }

  /**
   * 添加监控时段，不要添加过多的时间段，因为每个时间段都要消耗一份内存，一般加一个时间段就可以了
   * @param durations 单位：秒
   * @author: tanyaowu
   */
  public void addDurations(Long[] durations) {
    if (durations != null) {
      for (Long duration : durations) {
        addDuration(duration);
      }
    }
  }

  /**
   * 删除监控时间段
   * @param duration
   * @author: tanyaowu
   */
  public void removeDuration(Long duration) {
    clear(duration);
    cacheMap.remove(duration);

    if (CollUtil.isNotEmpty(durationList)) {
      durationList.remove(duration);
    }
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
    AbsCache cache = cacheMap.get(duration);
    if (cache == null) {
      return;
    }
    cache.clear();
  }

  /**
   * 根据ip获取IpStat，如果缓存中不存在，则创建
   * @param duration
   * @param channelContext
   * @return
   * @author: tanyaowu
   */
  public IpStat get(Long duration, ChannelContext channelContext) {
    return get(duration, channelContext, true);
  }

  /**
   * 根据ip获取IpStat，如果缓存中不存在，则根据forceCreate的值决定是否创建
   * @param duration
   * @param channelContext
   * @param forceCreate
   * @return
   * @author: tanyaowu
   */
  public IpStat get(Long duration, ChannelContext channelContext, boolean forceCreate) {
    return _get(duration, channelContext, forceCreate, true);
  }

  /**
   * 
   * @param duration
   * @param channelContext
   * @param forceCreate
   * @param useProxyClient
   * @return
   * @author tanyaowu
   */
  public IpStat _get(Long duration, ChannelContext channelContext, boolean forceCreate, boolean useProxyClient) {
    if (channelContext == null) {
      return null;
    }
    AbsCache cache = cacheMap.get(duration);
    if (cache == null) {
      return null;
    }

    String ip = null;
    if (useProxyClient && channelContext.getProxyClientNode() != null) {
      ip = channelContext.getProxyClientNode().getHost();
    } else {
      ip = channelContext.getClientNode().getHost();
    }
    IpStat ipStat = (IpStat) cache.get(ip);
    if (ipStat == null && forceCreate) {
      synchronized (this) {
        ipStat = (IpStat) cache.get(ip);
        if (ipStat == null) {
          ipStat = new IpStat(ip, duration);
          cache.put(ip, ipStat);
        }
      }
    }
    return ipStat;
  }

  /**
   *
   * @return
   * @author: tanyaowu
   */
  public Map<String, Serializable> map(Long duration) {
    AbsCache cache = cacheMap.get(duration);
    if (cache == null) {
      return null;
    }
    Map<String, Serializable> map = cache.asMap();
    return map;
  }

  /**
   *
   * @return
   * @author: tanyaowu
   */
  public Long size(Long duration) {
    AbsCache cache = cacheMap.get(duration);
    if (cache == null) {
      return null;
    }
    return cache.size();
  }

  /**
   *
   * @return
   * @author: tanyaowu
   */
  public Collection<Serializable> values(Long duration) {
    AbsCache cache = cacheMap.get(duration);
    if (cache == null) {
      return null;
    }
    Collection<Serializable> set = cache.asMap().values();
    return set;
  }
}
