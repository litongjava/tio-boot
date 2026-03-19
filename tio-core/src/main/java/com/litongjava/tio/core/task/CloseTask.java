package com.litongjava.tio.core.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.client.ClientChannelContext;
import com.litongjava.tio.client.ClientTioConfig;
import com.litongjava.tio.client.ReconnConf;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.maintain.MaintainUtils;
import com.litongjava.tio.utils.SystemTimer;

public class CloseTask {
  private static final Logger log = LoggerFactory.getLogger(CloseTask.class);
  public static void close(ChannelContext channelContext) {
    boolean isNeedRemove = channelContext.closeMeta.isNeedRemove;
    String remark = channelContext.closeMeta.remark;
    Throwable throwable = channelContext.closeMeta.throwable;
    channelContext.stat.timeClosed = SystemTimer.currTime;

    if (channelContext.tioConfig.getAioListener() != null) {
      try {
        channelContext.tioConfig.getAioListener().onBeforeClose(channelContext, throwable, remark, isNeedRemove);
      } catch (Throwable e) {
        channelContext.isWaitingClose = false;
        log.error(e.toString(), e);
      }
    }

    if (channelContext.isClosed && !isNeedRemove) {
      return;
    }

    if (channelContext.isRemoved) {
      return;
    }

    try {
      if (isNeedRemove) {
        MaintainUtils.remove(channelContext);
      } else {
        ClientTioConfig clientTioConfig = (ClientTioConfig) channelContext.tioConfig;
        clientTioConfig.closeds.add(channelContext);
        clientTioConfig.connecteds.remove(channelContext);
        MaintainUtils.close(channelContext);
      }

      channelContext.setRemoved(isNeedRemove);
      if (channelContext.tioConfig.statOn) {
        channelContext.tioConfig.groupStat.closed.incrementAndGet();
      }
      channelContext.stat.timeClosed = SystemTimer.currTime;
      channelContext.setClosed(true);
    } catch (Throwable e) {
      log.error(e.toString(), e);
    } finally {
      if (!isNeedRemove && channelContext.isClosed && !channelContext.isServer()) // 不删除且没有连接上，则加到重连队列中
      {
        ClientChannelContext clientChannelContext = (ClientChannelContext) channelContext;
        ReconnConf.put(clientChannelContext);
      }
      channelContext.isWaitingClose = false;
    }

  }
}
