package com.litongjava.tio.core;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.aio.PacketMeta;
import com.litongjava.model.func.Converter;
import com.litongjava.model.page.Page;
import com.litongjava.tio.client.ClientChannelContext;
import com.litongjava.tio.client.ClientTioConfig;
import com.litongjava.tio.client.ReconnConf;
import com.litongjava.tio.consts.TioCoreConfigKeys;
import com.litongjava.tio.core.task.CloseTask;
import com.litongjava.tio.core.task.SendPacketTask;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.lock.ReadLockHandler;
import com.litongjava.tio.utils.lock.SetWithLock;
import com.litongjava.tio.utils.page.PageUtils;

/**
 * The Class Tio. t-io用户关心的API几乎全在这
 * @author tanyaowu
 */
public class Tio {
  private static final Logger log = LoggerFactory.getLogger(Tio.class);
  private final static boolean DIAGNOSTIC_LOG_ENABLED = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_DIAGNOSTIC, false);

  /**
   * 绑定业务id
   * @param channelContext
   * @param bsId
   * @author tanyaowu
   */
  public static void bindBsId(ChannelContext channelContext, String bsId) {
    channelContext.tioConfig.bsIds.bind(channelContext, bsId);
  }

  /**
   * 绑定群组
   * @param channelContext
   * @param group
   * @author tanyaowu
   */
  public static void bindGroup(ChannelContext channelContext, String group) {
    channelContext.tioConfig.groups.bind(group, channelContext);
  }

  /**
   * 将用户绑定到群组
   * @param tioConfig
   * @param userid
   * @param group
   */
  public static void bindGroup(TioConfig tioConfig, String userid, String group) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByUserId(tioConfig, userid);
    if (setWithLock != null) {
      setWithLock.handle(new ReadLockHandler<Set<ChannelContext>>() {
        @Override
        public void handler(Set<ChannelContext> set) {
          for (ChannelContext channelContext : set) {
            Tio.bindGroup(channelContext, group);
          }
        }
      });
    }
  }

  /**
   * 绑定token
   * @param channelContext
   * @param token
   * @author tanyaowu
   */
  public static void bindToken(ChannelContext channelContext, String token) {
    channelContext.tioConfig.tokens.bind(token, channelContext);
  }

  /**
   * 绑定用户
   * @param channelContext
   * @param userIdStr
   */
  public static void bindUserId(ChannelContext channelContext, String userId) {
    channelContext.tioConfig.users.bind(userId, channelContext);
  }

  /**
   * 阻塞发送消息到指定ChannelContext
   * @param channelContext
   * @param packet
   */
  public static boolean bSend(ChannelContext channelContext, Packet packet) {
    if (channelContext == null) {
      return false;
    }
    CountDownLatch countDownLatch = new CountDownLatch(1);
    return send(channelContext, packet, countDownLatch, PacketSendMode.SINGLE_BLOCK);
  }

  /**
   * 发送到指定的ip和port
   * @param tioConfig
   * @param ip
   * @param port
   * @param packet
   * @author tanyaowu
   */
  public static Boolean bSend(TioConfig tioConfig, String ip, int port, Packet packet) {
    return send(tioConfig, ip, port, packet, true);
  }

  /**
   * 发消息到所有连接
   * @param tioConfig
   * @param packet
   * @param channelContextFilter
   * @author tanyaowu
   */
  public static Boolean bSendToAll(TioConfig tioConfig, Packet packet, ChannelContextFilter channelContextFilter) {
    return sendToAll(tioConfig, packet, channelContextFilter, true);
  }

  /**
   * 阻塞发消息给指定业务ID
   * @param tioConfig
   * @param bsId
   * @param packet
   * @author tanyaowu
   */
  public static Boolean bSendToBsId(TioConfig tioConfig, String bsId, Packet packet) {
    return sendToBsId(tioConfig, bsId, packet, true);
  }

  /**
   * 发消息到组
   * @param tioConfig
   * @param group
   * @param packet
   * @author tanyaowu
   */
  public static Boolean bSendToGroup(TioConfig tioConfig, String group, Packet packet) {
    return bSendToGroup(tioConfig, group, packet, null);
  }

  /**
   * 发消息到组
   * @param tioConfig
   * @param group
   * @param packet
   * @param channelContextFilter
   * @author tanyaowu
   */
  public static Boolean bSendToGroup(TioConfig tioConfig, String group, Packet packet, ChannelContextFilter channelContextFilter) {
    return sendToGroup(tioConfig, group, packet, channelContextFilter, true);
  }

  /**
   * 发消息给指定ChannelContext id
   * @param channelContextId
   * @param packet
   * @author tanyaowu
   */
  public static Boolean bSendToId(TioConfig tioConfig, String channelContextId, Packet packet) {
    return sendToId(tioConfig, channelContextId, packet, true);
  }

  /**
   * 阻塞发送到指定ip对应的集合
   * @param tioConfig
   * @param ip
   * @param packet
   * @author: tanyaowu
   */
  public static Boolean bSendToIp(TioConfig tioConfig, String ip, Packet packet) {
    return bSendToIp(tioConfig, ip, packet, null);
  }

  /**
   * 阻塞发送到指定ip对应的集合
   * @param tioConfig
   * @param ip
   * @param packet
   * @param channelContextFilter
   * @return
   * @author: tanyaowu
   */
  public static Boolean bSendToIp(TioConfig tioConfig, String ip, Packet packet, ChannelContextFilter channelContextFilter) {
    return sendToIp(tioConfig, ip, packet, channelContextFilter, true);
  }

  /**
   * 发消息到指定集合
   * @param tioConfig
   * @param setWithLock
   * @param packet
   * @param channelContextFilter
   * @author tanyaowu
   */
  public static Boolean bSendToSet(TioConfig tioConfig, SetWithLock<ChannelContext> setWithLock, Packet packet, ChannelContextFilter channelContextFilter) {
    return sendToSet(tioConfig, setWithLock, packet, channelContextFilter, true);
  }

  /**
   * 阻塞发消息到指定token
   * @param tioConfig
   * @param token
   * @param packet
   * @return
   * @author tanyaowu
   */
  public static Boolean bSendToToken(TioConfig tioConfig, String token, Packet packet) {
    return sendToToken(tioConfig, token, packet, true);
  }

  /**
   * 阻塞发消息给指定用户
   * @param tioConfig
   * @param userid
   * @param packet
   * @return
   * @author tanyaowu
   */
  public static Boolean bSendToUser(TioConfig tioConfig, String userid, Packet packet) {
    return sendToUser(tioConfig, userid, packet, true);
  }

  /**
   * 关闭连接
   * @param channelContext
   * @param remark
   * @author tanyaowu
   */
  public static void close(ChannelContext channelContext, String remark) {
    close(channelContext, null, remark);
  }

  /**
   * 
   * @param channelContext
   * @param remark
   * @param closeCode
   */
  public static void close(ChannelContext channelContext, String remark, ChannelCloseCode closeCode) {
    close(channelContext, null, remark, closeCode);
  }

  /**
   * 关闭连接
   * @param channelContext
   * @param throwable
   * @param remark
   * @author tanyaowu
   */
  public static void close(ChannelContext channelContext, Throwable throwable, String remark) {
    close(channelContext, throwable, remark, false);
  }

  public static void close(ChannelContext channelContext, Throwable throwable, String remark, ChannelCloseCode closeCode) {
    close(channelContext, throwable, remark, false, closeCode);
  }

  public static void close(ChannelContext channelContext, Throwable throwable, String remark, boolean isNeedRemove) {
    close(channelContext, throwable, remark, isNeedRemove, true);
  }

  public static void close(ChannelContext channelContext, Throwable throwable, String remark, boolean isNeedRemove, ChannelCloseCode closeCode) {
    close(channelContext, throwable, remark, isNeedRemove, true, closeCode);
  }

  public static void close(ChannelContext channelContext, Throwable throwable, String remark, boolean isNeedRemove, boolean needCloseLock) {
    close(channelContext, throwable, remark, isNeedRemove, needCloseLock, null);
  }

  /**
   * 
   * @param channelContext
   * @param throwable
   * @param remark
   * @param isNeedRemove
   * @param needCloseLock
   */
  public static void close(ChannelContext channelContext, Throwable throwable, String remark, boolean isNeedRemove, boolean needCloseLock, ChannelCloseCode closeCode) {
    if (channelContext == null) {
      return;
    }
    if (channelContext.isWaitingClose) {
      log.debug("{} Waiting to be closed", channelContext);
      return;
    }

    // 先立即取消各项任务，这样可防止有新的任务被提交进来
    WriteLock writeLock = null;
    if (needCloseLock) {
      writeLock = channelContext.closeLock.writeLock();

      boolean tryLock = writeLock.tryLock();
      if (!tryLock) {
        return;
      }
      channelContext.isWaitingClose = true;
      writeLock.unlock();
    } else {
      channelContext.isWaitingClose = true;
    }

    if (closeCode == null) {
      if (channelContext.getCloseCode() == ChannelCloseCode.INIT_STATUS) {
        channelContext.setCloseCode(ChannelCloseCode.NO_CODE);
      }
    } else {
      channelContext.setCloseCode(closeCode);
    }

    if (channelContext.asynchronousSocketChannel != null) {
      try {
        channelContext.asynchronousSocketChannel.shutdownInput();
      } catch (Throwable e) {
        // log.error(e.toString(), e);
      }
      try {
        channelContext.asynchronousSocketChannel.shutdownOutput();
      } catch (Throwable e) {
        // log.error(e.toString(), e);
      }
      try {
        channelContext.asynchronousSocketChannel.close();
      } catch (Throwable e) {
        // log.error(e.toString(), e);
      }
    }

    channelContext.closeMeta.setRemark(remark);
    channelContext.closeMeta.setThrowable(throwable);
    if (!isNeedRemove) {
      if (channelContext.isServer()) {
        isNeedRemove = true;
      } else {
        ClientChannelContext clientChannelContext = (ClientChannelContext) channelContext;
        if (!ReconnConf.isNeedReconn(clientChannelContext, false)) { // do not to send
          isNeedRemove = true;
        }
      }
    }

    if (DIAGNOSTIC_LOG_ENABLED) {
      log.info("close {},remark:{}", channelContext, remark);
    }
    channelContext.closeMeta.setNeedRemove(isNeedRemove);
    CloseTask.close(channelContext);
  }

  /**
   * 关闭连接
   * @param tioConfig
   * @param clientIp
   * @param clientPort
   * @param throwable
   * @param remark
   * @author tanyaowu
   */
  public static void close(TioConfig tioConfig, String clientIp, Integer clientPort, Throwable throwable, String remark) {
    ChannelContext channelContext = tioConfig.clientNodes.find(clientIp, clientPort);
    close(channelContext, throwable, remark);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param ip
   * @param remark
   * @return
   */
  public static void closeIp(TioConfig tioConfig, String ip, String remark) {
    closeIp(tioConfig, ip, remark, null);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param ip
   * @param remark
   * @param closeCode
   */
  public static void closeIp(TioConfig tioConfig, String ip, String remark, ChannelCloseCode closeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByIp(tioConfig, ip);
    closeSet(tioConfig, setWithLock, remark, closeCode);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param group
   * @param remark
   * @return
   */
  public static void closeGroup(TioConfig tioConfig, String group, String remark) {
    closeGroup(tioConfig, group, remark, null);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param group
   * @param remark
   * @param closeCode
   */
  public static void closeGroup(TioConfig tioConfig, String group, String remark, ChannelCloseCode closeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByGroup(tioConfig, group);
    closeSet(tioConfig, setWithLock, remark, closeCode);
  }

  /**
   * 关闭用户的所有连接
   * @param tioConfig
   * @param userid
   * @param remark
   * @return
   */
  public static void closeUser(TioConfig tioConfig, String userid, String remark) {
    closeUser(tioConfig, userid, remark, null);
  }

  /**
   * 关闭某用户的所有连接
   * @param tioConfig
   * @param userid
   * @param remark
   * @param closeCode
   */
  public static void closeUser(TioConfig tioConfig, String userid, String remark, ChannelCloseCode closeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByUserId(tioConfig, userid);
    closeSet(tioConfig, setWithLock, remark, closeCode);
  }

  /**
   * 关闭token的所有连接
   * @param tioConfig
   * @param token
   * @param remark
   * @return
   */
  public static void closeToken(TioConfig tioConfig, String token, String remark) {
    closeToken(tioConfig, token, remark, null);
  }

  /**
   * 关闭某token的所有连接
   * @param tioConfig
   * @param token
   * @param remark
   * @param closeCode
   */
  public static void closeToken(TioConfig tioConfig, String token, String remark, ChannelCloseCode closeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByToken(tioConfig, token);
    closeSet(tioConfig, setWithLock, remark, closeCode);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param ip
   * @param remark
   * @return
   */
  public static void removeIp(TioConfig tioConfig, String ip, String remark) {
    removeIp(tioConfig, ip, remark, null);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param ip
   * @param remark
   * @param removeCode
   */
  public static void removeIp(TioConfig tioConfig, String ip, String remark, ChannelCloseCode removeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByIp(tioConfig, ip);
    removeSet(tioConfig, setWithLock, remark, removeCode);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param group
   * @param remark
   * @return
   */
  public static void removeGroup(TioConfig tioConfig, String group, String remark) {
    removeGroup(tioConfig, group, remark, null);
  }

  /**
   * 关闭某群所有连接
   * @param tioConfig
   * @param group
   * @param remark
   * @param removeCode
   */
  public static void removeGroup(TioConfig tioConfig, String group, String remark, ChannelCloseCode removeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByGroup(tioConfig, group);
    removeSet(tioConfig, setWithLock, remark, removeCode);
  }

  /**
   * 关闭用户的所有连接
   * @param tioConfig
   * @param userid
   * @param remark
   * @return
   */
  public static void removeUser(TioConfig tioConfig, String userid, String remark) {
    removeUser(tioConfig, userid, remark, null);
  }

  /**
   * 关闭某用户的所有连接
   * @param tioConfig
   * @param userid
   * @param remark
   * @param removeCode
   */
  public static void removeUser(TioConfig tioConfig, String userid, String remark, ChannelCloseCode removeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByUserId(tioConfig, userid);
    removeSet(tioConfig, setWithLock, remark, removeCode);
  }

  /**
   * 关闭token的所有连接
   * @param tioConfig
   * @param token
   * @param remark
   * @return
   */
  public static void removeToken(TioConfig tioConfig, String token, String remark) {
    removeToken(tioConfig, token, remark, null);
  }

  /**
   * 关闭某token的所有连接
   * @param tioConfig
   * @param token
   * @param remark
   * @param removeCode
   */
  public static void removeToken(TioConfig tioConfig, String token, String remark, ChannelCloseCode removeCode) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByToken(tioConfig, token);
    removeSet(tioConfig, setWithLock, remark, removeCode);
  }

  /**
   * 关闭集合
   * @param tioConfig
   * @param setWithLock
   * @param remark
   * @param closeCode
   * @author tanyaowu
   */
  public static void closeSet(TioConfig tioConfig, SetWithLock<ChannelContext> setWithLock, String remark, ChannelCloseCode closeCode) {
    if (setWithLock != null) {
      setWithLock.handle(new ReadLockHandler<Set<ChannelContext>>() {
        @Override
        public void handler(Set<ChannelContext> set) {
          for (ChannelContext channelContext : set) {
            Tio.close(channelContext, remark, closeCode);
          }
        }
      });
    }
  }

  /**
   * 移除集合
   * @param tioConfig
   * @param setWithLock
   * @param remark
   * @param closeCode
   * @author tanyaowu
   */
  public static void removeSet(TioConfig tioConfig, SetWithLock<ChannelContext> setWithLock, String remark, ChannelCloseCode closeCode) {
    if (setWithLock != null) {
      setWithLock.handle(new ReadLockHandler<Set<ChannelContext>>() {
        @Override
        public void handler(Set<ChannelContext> set) {
          for (ChannelContext channelContext : set) {
            Tio.remove(channelContext, remark, closeCode);
          }
        }
      });
    }
  }

  /**
   * 获取所有连接，包括当前处于断开状态的
   * @param tioConfig
   * @return
   * @author tanyaowu
   */
  public static SetWithLock<ChannelContext> getAll(TioConfig tioConfig) {
    return tioConfig.connections;
  }

  /**
   * 获取所有连接，包括当前处于断开状态的
   * @param tioConfig
   * @return
   * @author tanyaowu
   * @deprecated 用getAll(TioConfig tioConfig)
   */
  public static SetWithLock<ChannelContext> getAllChannelContexts(TioConfig tioConfig) {
    return getAll(tioConfig);
  }

  /**
   * 此API仅供 tio client使用
   * 获取所有处于正常连接状态的连接
   * @param clientTioConfig
   * @return
   * @author tanyaowu
   */
  public static SetWithLock<ChannelContext> getConnecteds(ClientTioConfig clientTioConfig) {
    return clientTioConfig.connecteds;
  }

  /**
   * 此API仅供 tio client使用
   * 获取所有处于正常连接状态的连接
   * @param clientTioConfig
   * @return
   * @author tanyaowu
   * @deprecated 用getAllConnecteds(ClientTioConfig clientTioConfig)
   */
  public static SetWithLock<ChannelContext> getAllConnectedsChannelContexts(ClientTioConfig clientTioConfig) {
    return getConnecteds(clientTioConfig);
  }

  /**
   * 根据业务id找ChannelContext
   * @param tioConfig
   * @param bsId
   * @return
   * @author tanyaowu
   */
  public static ChannelContext getByBsId(TioConfig tioConfig, String bsId) {
    return tioConfig.bsIds.find(tioConfig, bsId);
  }

  /**
   * 根据业务id找ChannelContext
   * @param tioConfig
   * @param bsId
   * @return
   * @author tanyaowu
   * @deprecated 用getByBsId(TioConfig tioConfig, String bsId)
   */
  public static ChannelContext getChannelContextByBsId(TioConfig tioConfig, String bsId) {
    return getByBsId(tioConfig, bsId);
  }

  /**
   * 根据clientip和clientport获取ChannelContext
   * @param tioConfig
   * @param clientIp
   * @param clientPort
   * @return
   * @author tanyaowu
   */
  public static ChannelContext getByClientNode(TioConfig tioConfig, String clientIp, Integer clientPort) {
    return tioConfig.clientNodes.find(clientIp, clientPort);
  }

  /**
   * 根据clientip和clientport获取ChannelContext
   * @param tioConfig
   * @param clientIp
   * @param clientPort
   * @return
   * @author tanyaowu
   * @deprecated getByClientNode(tioConfig, clientIp, clientPort)
   */
  public static ChannelContext getChannelContextByClientNode(TioConfig tioConfig, String clientIp, Integer clientPort) {
    return getByClientNode(tioConfig, clientIp, clientPort);
  }

  /**
   * 根据ChannelContext.id获取ChannelContext
   * @param channelContextId
   * @return
   * @author tanyaowu
   */
  public static ChannelContext getByChannelContextId(TioConfig tioConfig, String channelContextId) {
    return tioConfig.ids.find(tioConfig, channelContextId);
  }

  /**
   * 根据ChannelContext.id获取ChannelContext
   * @param channelContextId
   * @return
   * @author tanyaowu
   * @deprecated 用getByChannelContextId(tioConfig, channelContextId)
   */
  public static ChannelContext getChannelContextById(TioConfig tioConfig, String channelContextId) {
    return getByChannelContextId(tioConfig, channelContextId);
  }

  /**
   * 获取一个组的所有客户端
   * @param tioConfig
   * @param group
   * @return
   * @author tanyaowu
   */
  public static SetWithLock<ChannelContext> getByGroup(TioConfig tioConfig, String group) {
    return tioConfig.groups.clients(tioConfig, group);
  }

  /**
   * 获取一个组的所有客户端
   * @param tioConfig
   * @param group
   * @return
   * @author tanyaowu
   * @deprecated 用getByGroup(tioConfig, group)
   */
  public static SetWithLock<ChannelContext> getChannelContextsByGroup(TioConfig tioConfig, String group) {
    return getByGroup(tioConfig, group);
  }

  /**
   * 根据token获取SetWithLock<ChannelContext>
   * @param tioConfig
   * @param token
   * @return
   * @author tanyaowu
   */
  public static SetWithLock<ChannelContext> getByToken(TioConfig tioConfig, String token) {
    return tioConfig.tokens.find(tioConfig, token);
  }

  /**
   * 根据客户端ip获取SetWithLock<ChannelContext>
   * @param tioConfig
   * @param ip
   * @return
   * @author tanyaowu
   */
  public static SetWithLock<ChannelContext> getByIp(TioConfig tioConfig, String ip) {
    return tioConfig.ips.clients(tioConfig, ip);
  }

  /**
   * 根据token获取SetWithLock<ChannelContext>
   * @param tioConfig
   * @param token
   * @return
   * @author tanyaowu
   * @deprecated 用getByToken(tioConfig, token)
   */
  public static SetWithLock<ChannelContext> getChannelContextsByToken(TioConfig tioConfig, String token) {
    return getByToken(tioConfig, token);
  }

  /**
   * 根据userid获取SetWithLock<ChannelContext>
   * @param tioConfig
   * @param userId
   * @return
   * @author tanyaowu
   */
  public static SetWithLock<ChannelContext> getByUserId(TioConfig tioConfig, String userId) {
    return tioConfig.users.find(tioConfig, userId);
  }

  /**
   * 根据userid获取SetWithLock<ChannelContext>
   * @param tioConfig
   * @param userid
   * @return
   */
  public static SetWithLock<ChannelContext> getChannelContextsByUserId(TioConfig tioConfig, String userid) {
    return getByUserId(tioConfig, userid);
  }

  /**
   *
   * @param tioConfig
   * @param pageIndex
   * @param pageSize
   * @return
   */
  public static Page<ChannelContext> getPageOfAll(TioConfig tioConfig, Integer pageIndex, Integer pageSize) {
    return getPageOfAll(tioConfig, pageIndex, pageSize, null);
  }

  /**
   * 
   * @param tioConfig
   * @param pageIndex
   * @param pageSize
   * @param converter
   * @return
   */
  public static <T> Page<T> getPageOfAll(TioConfig tioConfig, Integer pageIndex, Integer pageSize, Converter<T> converter) {
    SetWithLock<ChannelContext> setWithLock = Tio.getAllChannelContexts(tioConfig);
    return PageUtils.fromSetWithLock(setWithLock, pageIndex, pageSize, converter);
  }

  /**
   * 这个方法是给客户器端用的
   * @param clientTioConfig
   * @param pageIndex
   * @param pageSize
   * @return
   */
  public static Page<ChannelContext> getPageOfConnecteds(ClientTioConfig clientTioConfig, Integer pageIndex, Integer pageSize) {
    return getPageOfConnecteds(clientTioConfig, pageIndex, pageSize, null);
  }

  /**
   * 这个方法是给客户器端用的
   * @param clientTioConfig
   * @param pageIndex
   * @param pageSize
   * @param converter
   * @return
   * @author tanyaowu
   */
  public static <T> Page<T> getPageOfConnecteds(ClientTioConfig clientTioConfig, Integer pageIndex, Integer pageSize, Converter<T> converter) {
    SetWithLock<ChannelContext> setWithLock = Tio.getAllConnectedsChannelContexts(clientTioConfig);
    return PageUtils.fromSetWithLock(setWithLock, pageIndex, pageSize, converter);
  }

  /**
   *
   * @param tioConfig
   * @param group
   * @param pageIndex
   * @param pageSize
   * @return
   * @author tanyaowu
   */
  public static Page<ChannelContext> getPageOfGroup(TioConfig tioConfig, String group, Integer pageIndex, Integer pageSize) {
    return getPageOfGroup(tioConfig, group, pageIndex, pageSize, null);
  }

  /**
   * 
   * @param tioConfig
   * @param group
   * @param pageIndex
   * @param pageSize
   * @param converter
   * @return
   */
  public static <T> Page<T> getPageOfGroup(TioConfig tioConfig, String group, Integer pageIndex, Integer pageSize, Converter<T> converter) {
    SetWithLock<ChannelContext> setWithLock = Tio.getChannelContextsByGroup(tioConfig, group);
    return PageUtils.fromSetWithLock(setWithLock, pageIndex, pageSize, converter);
  }

  /**
   * 群组有多少个连接
   * @param tioConfig
   * @param group
   * @return
   */
  public static int groupCount(TioConfig tioConfig, String group) {
    SetWithLock<ChannelContext> setWithLock = tioConfig.groups.clients(tioConfig, group);
    if (setWithLock == null) {
      return 0;
    }

    Set<ChannelContext> set = setWithLock.getObj();
    if (set == null) {
      return 0;
    }

    return set.size();
  }

  /**
   * 某通道是否在某群组中
   * @param group
   * @param channelContext
   * @return true：在该群组
   * @author: tanyaowu
   */
  public static boolean isInGroup(String group, ChannelContext channelContext) {
    SetWithLock<String> setWithLock = channelContext.getGroups();
    if (setWithLock == null) {
      return false;
    }

    Set<String> set = setWithLock.getObj();
    if (set == null) {
      return false;
    }

    return set.contains(group);
  }

  /**
   * 
   * @param channelContext
   * @param remark
   */
  public static void remove(ChannelContext channelContext, String remark) {
    remove(channelContext, remark, null);
  }

  /**
   * 和close方法对应，只不过不再进行重连等维护性的操作
   * @param channelContext
   * @param remark
   * @param closeCode
   */
  public static void remove(ChannelContext channelContext, String remark, ChannelCloseCode closeCode) {
    remove(channelContext, null, remark, closeCode);
  }

  /**
   * 和close方法对应，只不过不再进行重连等维护性的操作
   * @param channelContext
   * @param throwable
   * @param remark
   */
  public static void remove(ChannelContext channelContext, Throwable throwable, String remark) {
    remove(channelContext, throwable, remark, (ChannelCloseCode) null);
  }

  /**
   * 和close方法对应，只不过不再进行重连等维护性的操作
   * @param channelContext
   * @param throwable
   * @param remark
   * @param closeCode
   */
  public static void remove(ChannelContext channelContext, Throwable throwable, String remark, ChannelCloseCode closeCode) {
    close(channelContext, throwable, remark, true, closeCode);
  }

  /**
   * 和close方法对应，只不过不再进行重连等维护性的操作
   * @param tioConfig
   * @param clientIp
   * @param clientPort
   * @param throwable
   * @param remark
   */
  public static void remove(TioConfig tioConfig, String clientIp, Integer clientPort, Throwable throwable, String remark) {
    remove(tioConfig, clientIp, clientPort, throwable, remark, (ChannelCloseCode) null);
  }

  /**
   * 删除clientip和clientPort为指定值的连接
   * @param tioConfig
   * @param clientIp
   * @param clientPort
   * @param throwable
   * @param remark
   * @param closeCode
   */
  public static void remove(TioConfig tioConfig, String clientIp, Integer clientPort, Throwable throwable, String remark, ChannelCloseCode closeCode) {
    ChannelContext channelContext = tioConfig.clientNodes.find(clientIp, clientPort);
    remove(channelContext, throwable, remark, closeCode);
  }

  /**
   * 删除clientip为指定值的所有连接
   * @param serverTioConfig
   * @param ip
   * @param remark
   */
  public static void remove(ServerTioConfig serverTioConfig, String ip, String remark) {
    remove(serverTioConfig, ip, remark, (ChannelCloseCode) null);
  }

  /**
   *  删除clientip为指定值的所有连接
   * @param serverTioConfig
   * @param ip
   * @param remark
   * @param closeCode
   */
  public static void remove(ServerTioConfig serverTioConfig, String ip, String remark, ChannelCloseCode closeCode) {
    SetWithLock<ChannelContext> setWithLock = serverTioConfig.ips.clients(serverTioConfig, ip);
    if (setWithLock == null) {
      return;
    }

    setWithLock.handle(new ReadLockHandler<Set<ChannelContext>>() {
      @Override
      public void handler(Set<ChannelContext> set) {
        for (ChannelContext channelContext : set) {
          Tio.remove(channelContext, remark, closeCode);
        }
      }
    });
  }

  /**
   * 发送消息到指定ChannelContext
   * @param channelContext
   * @param packet
   */
  public static boolean send(ChannelContext channelContext, Packet packet) {
    return send(channelContext, packet, null, null);
  }
  
  private static boolean send(final ChannelContext channelContext, Packet packet, CountDownLatch countDownLatch,
      //
      PacketSendMode packetSendMode) {
    if (packet == null || channelContext == null) {
      if (countDownLatch != null) {
        countDownLatch.countDown();
      }
      return false;
    }
    if (channelContext.isVirtual) {
      if (countDownLatch != null) {
        countDownLatch.countDown();
      }
      return true;
    }

    if (channelContext.isClosed || channelContext.isRemoved) {
      if (countDownLatch != null) {
        countDownLatch.countDown();
      }
      if (channelContext != null) {
        log.error("cancel send data {}, closed:{}, removed:{}", channelContext, channelContext.isClosed, channelContext.isRemoved);
      }
      return false;
    }

    String logstr = packet.logstr();
    if (channelContext.tioConfig.packetConverter != null) {
      packet = channelContext.tioConfig.packetConverter.convert(packet, channelContext);
      if (packet == null) {
        if (log.isInfoEnabled()) {
          log.info("Convert is null afterwards, indicating that no sending is required.", channelContext, logstr);
        }
        return true;
      }
    }
    boolean isSingleBlock = countDownLatch != null && packetSendMode == PacketSendMode.SINGLE_BLOCK;

    if (countDownLatch != null) {
      PacketMeta meta = new PacketMeta();
      meta.setCountDownLatch(countDownLatch);
      packet.setMeta(meta);
    }

    boolean sendInitiated = new SendPacketTask(channelContext).sendPacket(packet);

    if (!sendInitiated) {
      if (countDownLatch != null) {
        countDownLatch.countDown();
      }
      return false;
    }

    if (isSingleBlock) {
      long timeout = 10;
      try {
        Boolean awaitFlag = countDownLatch.await(timeout, TimeUnit.SECONDS);
        if (!awaitFlag) {
          log.error("{}, sync send timeout, timeout:{}s, packet:{}", channelContext, timeout, logstr);
        }
      } catch (InterruptedException e) {
        log.error(e.toString(), e);
      }

      Boolean isSentSuccess = packet.getMeta().getIsSentSuccess();
      return isSentSuccess;
    } else {
      return true;
    }
  }

  /**
   * 发送到指定的ip和port
   * @param tioConfig
   * @param ip
   * @param port
   * @param packet
   * @author tanyaowu
   */
  public static Boolean send(TioConfig tioConfig, String ip, int port, Packet packet) {
    return send(tioConfig, ip, port, packet, false);
  }

  /**
   * 发送到指定的ip和port
   * @param tioConfig
   * @param ip
   * @param port
   * @param packet
   * @param isBlock
   * @return
   * @author tanyaowu
   */
  private static Boolean send(TioConfig tioConfig, String ip, int port, Packet packet, boolean isBlock) {
    ChannelContext channelContext = tioConfig.clientNodes.find(ip, port);
    if (channelContext != null) {
      if (isBlock) {
        return bSend(channelContext, packet);
      } else {
        return send(channelContext, packet);
      }
    } else {
      log.info("{}, can find channelContext by {}:{}", tioConfig.getName(), ip, port);
      return false;
    }
  }

  public static void sendToAll(TioConfig tioConfig, Packet packet) {
    sendToAll(tioConfig, packet, null);
  }

  /**
   * 发消息到所有连接
   * @param tioConfig
   * @param packet
   * @param channelContextFilter
   * @author tanyaowu
   */
  public static void sendToAll(TioConfig tioConfig, Packet packet, ChannelContextFilter channelContextFilter) {
    sendToAll(tioConfig, packet, channelContextFilter, false);
  }

  /**
   *
   * @param tioConfig
   * @param packet
   * @param channelContextFilter
   * @param isBlock
   * @author tanyaowu
   */
  private static Boolean sendToAll(TioConfig tioConfig, Packet packet, ChannelContextFilter channelContextFilter, boolean isBlock) {
    try {
      SetWithLock<ChannelContext> setWithLock = tioConfig.connections;
      if (setWithLock == null) {
        log.debug("{}, No any connection.", tioConfig.getName());
        return false;
      }
      Boolean ret = sendToSet(tioConfig, setWithLock, packet, channelContextFilter, isBlock);
      return ret;
    } finally {
    }
  }

  /**
   * 发消息给指定业务ID
   * @param tioConfig
   * @param bsId
   * @param packet
   * @return
   * @author tanyaowu
   */
  public static Boolean sendToBsId(TioConfig tioConfig, String bsId, Packet packet) {
    return sendToBsId(tioConfig, bsId, packet, false);
  }

  /**
   * 发消息给指定业务ID
   * @param tioConfig
   * @param bsId
   * @param packet
   * @param isBlock
   * @return
   * @author tanyaowu
   */
  private static Boolean sendToBsId(TioConfig tioConfig, String bsId, Packet packet, boolean isBlock) {
    ChannelContext channelContext = Tio.getByBsId(tioConfig, bsId);
    if (channelContext == null) {
      return false;
    }
    if (isBlock) {
      return bSend(channelContext, packet);
    } else {
      return send(channelContext, packet);
    }
  }

  /**
   * 发消息到组
   * @param tioConfig
   * @param group
   * @param packet
   * @author tanyaowu
   */
  public static void sendToGroup(TioConfig tioConfig, String group, Packet packet) {
    sendToGroup(tioConfig, group, packet, null);
  }

  /**
   * 发消息到组
   * @param tioConfig
   * @param group
   * @param packet
   * @param channelContextFilter
   * @author tanyaowu
   */
  public static void sendToGroup(TioConfig tioConfig, String group, Packet packet, ChannelContextFilter channelContextFilter) {
    sendToGroup(tioConfig, group, packet, channelContextFilter, false);
  }

  /**
   * 发消息到组
   * @param tioConfig
   * @param group
   * @param packet
   * @param channelContextFilter
   * @param isBlock
   * @return
   */
  private static Boolean sendToGroup(TioConfig tioConfig, String group, Packet packet, ChannelContextFilter channelContextFilter, boolean isBlock) {
    try {
      SetWithLock<ChannelContext> setWithLock = tioConfig.groups.clients(tioConfig, group);
      if (setWithLock == null) {
        log.debug("{}, grup [{}] not exists", tioConfig.getName(), group);
        return false;
      }
      Boolean ret = sendToSet(tioConfig, setWithLock, packet, channelContextFilter, isBlock);
      return ret;
    } finally {
    }
  }

  /**
   * 发消息给指定ChannelContext id
   * @param channelContextId
   * @param packet
   * @author tanyaowu
   */
  public static Boolean sendToId(TioConfig tioConfig, String channelContextId, Packet packet) {
    return sendToId(tioConfig, channelContextId, packet, false);
  }

  /**
   * 发消息给指定ChannelContext id
   * @param channelContextId
   * @param packet
   * @param isBlock
   * @return
   * @author tanyaowu
   */
  private static Boolean sendToId(TioConfig tioConfig, String channelContextId, Packet packet, boolean isBlock) {
    ChannelContext channelContext = Tio.getChannelContextById(tioConfig, channelContextId);
    if (channelContext == null) {
      return false;
    }
    if (isBlock) {
      return bSend(channelContext, packet);
    } else {
      return send(channelContext, packet);
    }
  }

  /**
   * 发送到指定ip对应的集合
   * @param tioConfig
   * @param ip
   * @param packet
   * @author: tanyaowu
   */
  public static void sendToIp(TioConfig tioConfig, String ip, Packet packet) {
    sendToIp(tioConfig, ip, packet, null);
  }

  /**
   * 发送到指定ip对应的集合
   * @param tioConfig
   * @param ip
   * @param packet
   * @param channelContextFilter
   * @author: tanyaowu
   */
  public static void sendToIp(TioConfig tioConfig, String ip, Packet packet, ChannelContextFilter channelContextFilter) {
    sendToIp(tioConfig, ip, packet, channelContextFilter, false);
  }

  /**
   * 发送到指定ip对应的集合
   * @param tioConfig
   * @param ip
   * @param packet
   * @param channelContextFilter
   * @param isBlock
   * @return
   * @author: tanyaowu
   */
  private static Boolean sendToIp(TioConfig tioConfig, String ip, Packet packet, ChannelContextFilter channelContextFilter, boolean isBlock) {
    try {
      SetWithLock<ChannelContext> setWithLock = tioConfig.ips.clients(tioConfig, ip);
      if (setWithLock == null) {
        log.info("{}, 没有ip为[{}]的对端", tioConfig.getName(), ip);
        return false;
      }
      Boolean ret = sendToSet(tioConfig, setWithLock, packet, channelContextFilter, isBlock);
      return ret;
    } finally {
    }
  }

  /**
   * 发消息到指定集合
   * @param tioConfig
   * @param setWithLock
   * @param packet
   * @param channelContextFilter
   * @author tanyaowu
   */
  public static void sendToSet(TioConfig tioConfig, SetWithLock<ChannelContext> setWithLock, Packet packet, ChannelContextFilter channelContextFilter) {
    sendToSet(tioConfig, setWithLock, packet, channelContextFilter, false);
  }

  /**
   * 发消息到指定集合
   * @param tioConfig
   * @param setWithLock
   * @param packet
   * @param channelContextFilter
   * @param isBlock
   * @author tanyaowu
   */
  private static Boolean sendToSet(TioConfig tioConfig, SetWithLock<ChannelContext> setWithLock, Packet packet, ChannelContextFilter channelContextFilter, boolean isBlock) {
    boolean releasedLock = false;
    Lock lock = setWithLock.readLock();
    lock.lock();
    try {
      Set<ChannelContext> set = setWithLock.getObj();
      if (set.size() == 0) {
        log.debug("{}, 集合为空", tioConfig.getName());
        return false;
      }

      CountDownLatch countDownLatch = null;
      if (isBlock) {
        countDownLatch = new CountDownLatch(set.size());
      }
      int sendCount = 0;
      for (ChannelContext channelContext : set) {
        if (channelContextFilter != null) {
          boolean isfilter = channelContextFilter.filter(channelContext);
          if (!isfilter) {
            if (isBlock) {
              countDownLatch.countDown();
            }
            continue;
          }
        }

        sendCount++;
        if (isBlock) {
          send(channelContext, packet, countDownLatch, PacketSendMode.GROUP_BLOCK);
        } else {
          send(channelContext, packet, null, null);
        }
      }
      lock.unlock();
      releasedLock = true;

      if (sendCount == 0) {
        return false;
      }

      if (isBlock) {
        try {
          long timeout = sendCount / 5;
          timeout = Math.max(timeout, 10);// timeout < 10 ? 10 : timeout;
          boolean awaitFlag = countDownLatch.await(timeout, TimeUnit.SECONDS);
          if (!awaitFlag) {
            log.error("{}, 同步群发超时, size:{}, timeout:{}, packet:{}", tioConfig.getName(), setWithLock.getObj().size(), timeout, packet.logstr());
            return false;
          } else {
            return true;
          }
        } catch (InterruptedException e) {
          log.error(e.toString(), e);
          return false;
        } finally {

        }
      } else {
        return true;
      }
    } catch (Throwable e) {
      log.error(e.toString(), e);
      return false;
    } finally {
      if (!releasedLock) {
        lock.unlock();
      }
    }
  }

  /**
   * 发消息到指定token
   * @param tioConfig
   * @param token
   * @param packet
   * @return
   * @author tanyaowu
   */
  public static Boolean sendToToken(TioConfig tioConfig, String token, Packet packet) {
    return sendToToken(tioConfig, token, packet, false);
  }

  /**
   * 发消息给指定token
   * @param tioConfig
   * @param token
   * @param packet
   * @param isBlock
   * @author tanyaowu
   */
  private static Boolean sendToToken(TioConfig tioConfig, String token, Packet packet, boolean isBlock) {
    SetWithLock<ChannelContext> setWithLock = tioConfig.tokens.find(tioConfig, token);
    try {
      if (setWithLock == null) {
        return false;
      }

      ReadLock readLock = setWithLock.readLock();
      readLock.lock();
      try {
        Set<ChannelContext> set = setWithLock.getObj();
        boolean ret = false;
        for (ChannelContext channelContext : set) {
          boolean singleRet = false;
          // 不要用 a = a || b()，容易漏执行后面的函数
          if (isBlock) {
            singleRet = bSend(channelContext, packet);
          } else {
            singleRet = send(channelContext, packet);
          }
          if (singleRet) {
            ret = true;
          }
        }
        return ret;
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      } finally {
        readLock.unlock();
      }
      return false;
    } finally {
    }
  }

  /**
   * 发消息给指定用户
   * @param tioConfig
   * @param userid
   * @param packet
   * @author tanyaowu
   */
  public static Boolean sendToUser(TioConfig tioConfig, String userid, Packet packet) {
    return sendToUser(tioConfig, userid, packet, false);
  }

  /**
   * 发消息给指定用户
   * @param tioConfig
   * @param userid
   * @param packet
   * @param isBlock
   * @author tanyaowu
   */
  private static Boolean sendToUser(TioConfig tioConfig, String userid, Packet packet, boolean isBlock) {
    SetWithLock<ChannelContext> setWithLock = tioConfig.users.find(tioConfig, userid);
    try {
      if (setWithLock == null) {
        return false;
      }

      ReadLock readLock = setWithLock.readLock();
      readLock.lock();
      try {
        Set<ChannelContext> set = setWithLock.getObj();
        boolean ret = false;
        for (ChannelContext channelContext : set) {
          boolean singleRet = false;
          // 不要用 a = a || b()，容易漏执行后面的函数
          if (isBlock) {
            singleRet = bSend(channelContext, packet);
          } else {
            singleRet = send(channelContext, packet);
          }
          if (singleRet) {
            ret = true;
          }
        }
        return ret;
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      } finally {
        readLock.unlock();
      }
      return false;
    } finally {
    }
  }

  /**
   * 解绑业务id
   * @param channelContext
   * @author tanyaowu
   */
  public static void unbindBsId(ChannelContext channelContext) {
    channelContext.tioConfig.bsIds.unbind(channelContext);
  }

  /**
   * 与所有组解除解绑关系
   * @param channelContext
   * @author tanyaowu
   */
  public static void unbindGroup(ChannelContext channelContext) {
    channelContext.tioConfig.groups.unbind(channelContext);
  }

  /**
   * 与指定组解除绑定关系
   * @param group
   * @param channelContext
   * @author tanyaowu
   */
  public static void unbindGroup(String group, ChannelContext channelContext) {
    channelContext.tioConfig.groups.unbind(group, channelContext);
  }

  /**
   * 将某用户从组中解除绑定
   * @param tioConfig
   * @param userid
   * @param group
   */
  public static void unbindGroup(TioConfig tioConfig, String userid, String group) {
    SetWithLock<ChannelContext> setWithLock = Tio.getByUserId(tioConfig, userid);
    if (setWithLock != null) {
      setWithLock.handle(new ReadLockHandler<Set<ChannelContext>>() {
        @Override
        public void handler(Set<ChannelContext> set) {
          for (ChannelContext channelContext : set) {
            Tio.unbindGroup(group, channelContext);
          }
        }
      });
    }
  }

  /**
   * 解除channelContext绑定的token
   * @param channelContext
   * @author tanyaowu
   */
  public static void unbindToken(ChannelContext channelContext) {
    channelContext.tioConfig.tokens.unbind(channelContext);
  }

  /**
   * 解除token
   * @param tioConfig
   * @param token
   */
  public static void unbindToken(TioConfig tioConfig, String token) {
    tioConfig.tokens.unbind(tioConfig, token);
  }

  // org.tio.core.TioConfig.ipBlacklist

  /**
   * 解除channelContext绑定的userid
   * @param channelContext
   * @author tanyaowu
   */
  public static void unbindUser(ChannelContext channelContext) {
    channelContext.tioConfig.users.unbind(channelContext);
  }

  /**
   * 解除userid的绑定。一般用于多地登录，踢掉前面登录的场景
   * @param tioConfig
   * @param userid
   * @author: tanyaowu
   */
  public static void unbindUser(TioConfig tioConfig, String userid) {
    tioConfig.users.unbind(tioConfig, userid);
  }

  private Tio() {
  }

}
