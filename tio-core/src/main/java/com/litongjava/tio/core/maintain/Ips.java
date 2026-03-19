package com.litongjava.tio.core.maintain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lock.MapWithLock;
import com.litongjava.tio.utils.lock.SetWithLock;

/**
 * 一对多  (ip <--> ChannelContext)<br>
 * 一个ip有哪些客户端，该维护只在Server侧有<br>
 */
public class Ips {

  /** The log. */
  private static Logger log = LoggerFactory.getLogger(Ips.class);

  /** 一个IP有哪些客户端
   * key: ip
   * value: SetWithLock<ChannelContext>
   */
  private final MapWithLock<String, SetWithLock<ChannelContext>> ipmap = new MapWithLock<>(new HashMap<String, SetWithLock<ChannelContext>>());
  private final ConcurrentHashMap<String, Object> ipLocks = new ConcurrentHashMap<>();

  /**
   * 和ip绑定
   * @param channelContext
   */
  public void bind(ChannelContext channelContext) {
    if (channelContext == null) {
      return;
    }

    if (channelContext.tioConfig.isShortConnection) {
      return;
    }

    String ip = channelContext.getClientNode().getHost();
    if (ChannelContext.UNKNOWN_ADDRESS_IP.equals(ip)) {
      return;
    }

    if (StrUtil.isBlank(ip)) {
      return;
    }

    SetWithLock<ChannelContext> channelSet = ipmap.get(ip);
    try {
      if (channelSet == null) {
        Object lock = ipLocks.computeIfAbsent(ip, k -> new Object());

        synchronized (lock) {
          // 双重检查锁定
          channelSet = ipmap.get(ip);
          if (channelSet == null) {
            channelSet = new SetWithLock<>(new HashSet<>());
            ipmap.put(ip, channelSet);
          }
        }
      }
      channelSet.add(channelContext);
    } catch (Exception e) {
      log.error("绑定ChannelContext时出错: {}", e.toString(), e);
    }
  }

  /**
   * 一个ip有哪些客户端，有可能返回null
   * @param tioConfig
   * @param ip
   * @return
   */
  public SetWithLock<ChannelContext> clients(TioConfig tioConfig, String ip) {
    if (tioConfig.isShortConnection) {
      return null;
    }

    if (StrUtil.isBlank(ip)) {
      return null;
    }
    return ipmap.get(ip);
  }

  /**
   * @return the ipmap
   */
  public MapWithLock<String, SetWithLock<ChannelContext>> getIpmap() {
    return ipmap;
  }

  /**
   * 与指定ip解除绑定
   * @param channelContext
   */
  public void unbind(ChannelContext channelContext) {
    if (channelContext == null) {
      return;
    }

    if (channelContext.tioConfig.isShortConnection) {
      return;
    }

    try {
      String ip = channelContext.getClientNode().getHost();
      if (StrUtil.isBlank(ip)) {
        return;
      }
      if (ChannelContext.UNKNOWN_ADDRESS_IP.equals(ip)) {
        return;
      }

      SetWithLock<ChannelContext> channelSet = ipmap.get(ip);
      if (channelSet != null) {
        channelSet.remove(channelContext);
        if (channelSet.size() == 0) {
          Object lock = ipLocks.computeIfAbsent(ip, k -> new Object());
          synchronized (lock) {
            // 再次检查，确保没有新的连接绑定到该 IP
            if (channelSet.size() == 0) {
              ipmap.remove(ip);
              ipLocks.remove(ip); // 移除锁对象，防止内存泄漏
            }
          }
        }
      } else {
        log.debug("{}, ip【{}】 找不到对应的SetWithLock", channelContext.tioConfig.getName(), ip);
      }
    } catch (Exception e) {
      log.error("解除绑定ChannelContext时出错: {}", e.toString(), e);
    }
  }
}
