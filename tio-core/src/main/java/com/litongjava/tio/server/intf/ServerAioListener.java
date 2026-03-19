package com.litongjava.tio.server.intf;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.intf.AioListener;

/**
 *
 * @author tanyaowu
 *
 */
public interface ServerAioListener extends AioListener {

  /**
   * 
   * 服务器检查到心跳超时时，会调用这个函数（一般场景，该方法只需要直接返回false即可）
   * 
   * @param channelContext
   * @param interval              已经多久没有收发消息了，单位：毫秒
   * @param heartbeatTimeoutCount 心跳超时次数，第一次超时此值是1，以此类推。此值被保存在：channelContext.stat.heartbeatTimeoutCount
   * @return 返回true，那么服务器则不关闭此连接；返回false，服务器将按心跳超时关闭该连接
   */
  public boolean onHeartbeatTimeout(ChannelContext channelContext, Long interval, int heartbeatTimeoutCount);
}
