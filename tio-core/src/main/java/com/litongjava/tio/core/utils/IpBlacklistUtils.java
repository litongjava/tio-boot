package com.litongjava.tio.core.utils;

import java.util.Collection;

import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.maintain.GlobalIpBlacklist;

public class IpBlacklistUtils {
  /**
   * 把ip添加到黑名单，此黑名单只针对tioConfig有效，其它tioConfig不会把这个ip视为黑名单
   * @param tioConfig
   * @param ip
   */
  public static boolean add(TioConfig tioConfig, String ip) {
    return tioConfig.ipBlacklist.add(ip);
  }

  /**
   * 添加全局ip黑名单
   * @param ip
   * @return
   * @author tanyaowu
   */
  public static boolean add(String ip) {
    return GlobalIpBlacklist.INSTANCE.global.add(ip);
  }

  /**
   * 清空黑名单，只针对tioConfig有效
   * @param tioConfig
   * @author tanyaowu
   */
  public static void clear(TioConfig tioConfig) {
    tioConfig.ipBlacklist.clear();
  }

  /**
   * 清空全局黑名单
   * @author tanyaowu
   */
  public static void clear() {
    GlobalIpBlacklist.INSTANCE.global.clear();
  }

  /**
   * 获取ip黑名单列表
   * @param tioConfig
   * @return
   * @author tanyaowu
   */
  public static Collection<String> getAll(TioConfig tioConfig) {
    return tioConfig.ipBlacklist.getAll();
  }

  /**
   * 获取全局黑名单
   * @return
   * @author tanyaowu
   */
  public static Collection<String> getAll() {
    return GlobalIpBlacklist.INSTANCE.global.getAll();
  }

  /**
   * 是否在黑名单中
   * @param tioConfig
   * @param ip
   * @return
   * @author tanyaowu
   */
  public static boolean isInBlacklist(TioConfig tioConfig, String ip) {
    if (tioConfig.ipBlacklist != null) {
      return tioConfig.ipBlacklist.isInBlacklist(ip) || GlobalIpBlacklist.INSTANCE.global.isInBlacklist(ip);

    } else {
      return GlobalIpBlacklist.INSTANCE.global.isInBlacklist(ip);
    }

  }

  /**
   * 把ip从黑名单中删除
   * @param tioConfig
   * @param ip
   * @author tanyaowu
   */
  public static void remove(TioConfig tioConfig, String ip) {
    tioConfig.ipBlacklist.remove(ip);
  }

  /**
   * 删除全局黑名单
   * @param ip
   * @author tanyaowu
   */
  public static void remove(String ip) {
    GlobalIpBlacklist.INSTANCE.global.remove(ip);
  }
}
