package com.litongjava.tio.server;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.enhance.buffer.BufferMemoryStat;
import com.litongjava.enhance.buffer.BufferMomeryInfo;
import com.litongjava.enhance.buffer.GlobalScheduler;
import com.litongjava.model.sys.SysConst;
import com.litongjava.tio.consts.TioCoreConfigKeys;
import com.litongjava.tio.core.ChannelCloseCode;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.intf.AioHandler;
import com.litongjava.tio.core.intf.AioListener;
import com.litongjava.tio.core.maintain.GlobalIpBlacklist;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.core.ssl.SslConfig;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.server.intf.ServerAioListener;
import com.litongjava.tio.utils.AppendJsonConverter;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.executor.TioThreadPoolStats;
import com.litongjava.tio.utils.hutool.CollUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lock.SetWithLock;

/**
 * 
 * @author tanyaowu 2016年10月10日 下午5:51:56
 */
public class ServerTioConfig extends TioConfig {
  private static final Logger log = LoggerFactory.getLogger(ServerTioConfig.class);
  private ServerAioHandler serverAioHandler = null;
  private ServerAioListener serverAioListener = null;
  private Thread checkHeartbeatThread = null;
  private boolean needCheckHeartbeat = true;
  private boolean isShared = false;
  /**
   * 服务端backlog
   */
  private int backlog = 1000;

  public ServerTioConfig(String name) {
    super(name);
  }

  /**
   * 
   * @param keyStoreFile   如果是以"classpath:"开头，则从classpath中查找，否则视为普通的文件路径
   * @param trustStoreFile 如果是以"classpath:"开头，则从classpath中查找，否则视为普通的文件路径
   * @param keyStorePwd
   * @throws FileNotFoundException
   */
  public void useSsl(String keyStoreFile, String trustStoreFile, String keyStorePwd) throws Exception {
    if (StrUtil.isNotBlank(keyStoreFile) && StrUtil.isNotBlank(trustStoreFile)) {
      SslConfig sslConfig = SslConfig.forServer(keyStoreFile, trustStoreFile, keyStorePwd);
      this.setSslConfig(sslConfig);
    }
  }

  /**
   * 
   * @param keyStoreInputStream
   * @param trustStoreInputStream
   * @param passwd
   * @throws Exception
   * @author tanyaowu
   */
  public void useSsl(InputStream keyStoreInputStream, InputStream trustStoreInputStream, String passwd) throws Exception {
    SslConfig sslConfig = SslConfig.forServer(keyStoreInputStream, trustStoreInputStream, passwd);
    this.setSslConfig(sslConfig);
  }

  /**
   * @see org.tio.core.TioConfig#getAioHandler()
   *
   * @return
   * @author tanyaowu 2016年12月20日 上午11:34:37
   *
   */
  @Override
  public AioHandler getAioHandler() {
    return this.getServerAioHandler();
  }

  /**
   * @see org.tio.core.TioConfig#getAioListener()
   *
   * @return
   * @author tanyaowu 2016年12月20日 上午11:34:37
   *
   */
  @Override
  public AioListener getAioListener() {
    return getServerAioListener();
  }

  /**
   * @return the serverAioHandler
   */
  public ServerAioHandler getServerAioHandler() {
    return serverAioHandler;
  }

  /**
   * @return the serverAioListener
   */
  public ServerAioListener getServerAioListener() {
    return serverAioListener;
  }

  public void setServerAioListener(ServerAioListener serverAioListener) {
    this.serverAioListener = serverAioListener;
  }

  /**
   * @return
   * @author tanyaowu
   */
  @Override
  public boolean isServer() {
    return true;
  }

  @Override
  public String toString() {
    return "ServerTioConfig [name=" + name + "]";
  }

  public void share(ServerTioConfig tioConfig) {
    synchronized (ServerTioConfig.class) {
      if (tioConfig == this) {
        return;
      }
      this.clientNodes = tioConfig.clientNodes;
      this.connections = tioConfig.connections;
      this.groups = tioConfig.groups;
      this.users = tioConfig.users;
      this.tokens = tioConfig.tokens;
      this.ids = tioConfig.ids;
      this.bsIds = tioConfig.bsIds;
      this.ipBlacklist = tioConfig.ipBlacklist;
      this.ips = tioConfig.ips;

      if (!tioConfig.isShared && !this.isShared) {
        this.needCheckHeartbeat = false;
      }
      if (tioConfig.isShared && !this.isShared) {
        this.needCheckHeartbeat = false;
      }
      if (!tioConfig.isShared && this.isShared) {
        tioConfig.needCheckHeartbeat = false;
      }

      // 下面这两行代码要放到前面if的后面
      tioConfig.isShared = true;
      this.isShared = true;
    }
  }

  public void setServerAioHandler(ServerAioHandler serverAioHandler) {
    this.serverAioHandler = serverAioHandler;
  }

  public int getBacklog() {
    return backlog;
  }

  public void setBacklog(int backlog) {
    this.backlog = backlog;
  }

  public void init() {
    super.init();
    this.groupStat = new ServerGroupStat();
    GlobalIpBlacklist.INSTANCE.init(this);
    if (needCheckHeartbeat && heartbeatTimeout > 0) {
      startHeartbeatCheck();
    }
    if (printStats) {
      GlobalScheduler.scheduleWithFixedDelay(this::printStats, 60, 60, TimeUnit.SECONDS);
    }

  }

  private void startHeartbeatCheck() {
    Runnable check = new Runnable() {
      @Override
      public void run() {
        // 第一次先休息一下
        try {
          Thread.sleep(1000 * 10);
        } catch (InterruptedException e1) {
          log.error(e1.toString(), e1);
        }

        while (needCheckHeartbeat && !isStopped()) {
          // long sleeptime = heartbeatTimeout;
          if (heartbeatTimeout <= 0) {
            break;
          }
          try {
            Thread.sleep(heartbeatTimeout);
          } catch (InterruptedException e1) {
            log.error(e1.toString(), e1);
          }
          SetWithLock<ChannelContext> setWithLock = connections;
          Set<ChannelContext> set = null;
          ReadLock readLock = setWithLock.readLock();
          readLock.lock();
          try {
            set = setWithLock.getObj();

            for (ChannelContext channelContext : set) {
              long compareTime = Math.max(channelContext.stat.latestTimeOfReceivedByte, channelContext.stat.latestTimeOfSentPacket);
              long currtime = SystemTimer.currTime;
              long interval = currtime - compareTime;

              boolean needRemove = false;
              if (channelContext.heartbeatTimeout != null && channelContext.heartbeatTimeout > 0) {
                needRemove = interval > channelContext.heartbeatTimeout;
              } else {
                needRemove = interval > heartbeatTimeout;
              }

              if (needRemove) {
                if (!ServerTioConfig.this.serverAioListener.onHeartbeatTimeout(channelContext, interval,
                    channelContext.stat.heartbeatTimeoutCount.incrementAndGet())) {
                  log.info("{}, {} ms or not send and receive message", channelContext, interval);
                  channelContext.setCloseCode(ChannelCloseCode.HEARTBEAT_TIMEOUT);
                  Tio.remove(channelContext, interval + " ms not send and receive message");
                }
              }
            }
          } catch (Throwable e) {
            log.error(e.getMessage(), e);
          } finally {
            try {
              readLock.unlock();
            } catch (Throwable e) {
              log.error(e.getMessage(), e);
            }
          }
        }
      }
    };

    checkHeartbeatThread = new Thread(check, "tio-timer-checkheartbeat-" + id + "-" + name);
    checkHeartbeatThread.setDaemon(true);
    checkHeartbeatThread.setPriority(Thread.MIN_PRIORITY);
    checkHeartbeatThread.start();
  }

  private void printStats() {
    StringBuilder builder = new StringBuilder();
    builder.append(SysConst.CRLF).append(name);
    builder.append("\r\n ├ Current Time: ").append(SystemTimer.currTime);

    builder.append("\r\n ├ Connection Statistics");
    builder.append("\r\n │ \t ├ Total Accepted Connections: ").append(((ServerGroupStat) groupStat).accepted.get());
    builder.append("\r\n │ \t ├ Current Connections: ").append(this.connections.getObj().size());
    builder.append("\r\n │ \t ├ Unique IP Connections: ").append(this.ips.getIpmap().getObj().size());
    builder.append("\r\n │ \t └ Closed Connections: ").append(groupStat.closed.get());

    builder.append("\r\n ├ Message Statistics");
    builder.append("\r\n │ \t ├ Processed Messages: ").append(groupStat.handledPackets.get());
    builder.append("\r\n │ \t ├ Received Messages (packet/byte): ").append(groupStat.receivedPackets.get()).append("/")
        .append(groupStat.receivedBytes.get());
    builder.append("\r\n │ \t ├ Sent Messages (packet/byte): ").append(groupStat.sentPackets.get()).append("/")
        .append(groupStat.sentBytes.get()).append("b");
    builder.append("\r\n │ \t ├ Avg Bytes Per TCP Receive: ").append(groupStat.getBytesPerTcpReceive());
    builder.append("\r\n │ \t └ Avg Packets Per TCP Receive: ").append(groupStat.getPacketsPerTcpReceive());

    builder.append("\r\n └ IP Statistics Duration");
    if (CollUtil.isNotEmpty(ipStats.durationList)) {
      builder.append("\r\n   \t └ ").append(AppendJsonConverter.convertListLongToJson(this.ipStats.durationList));
    } else {
      builder.append("\r\n   \t └ ").append("No IP statistics duration set");
    }

    builder.append("\r\n ├ Node Statistics");
    builder.append("\r\n │ \t ├ Client Nodes: ").append(this.clientNodes.getObjWithLock().getObj().size());
    builder.append("\r\n │ \t ├ All Connections: ").append(this.connections.getObj().size());
    builder.append("\r\n │ \t ├ Bound Users: ").append(this.users.getMap().getObj().size());
    builder.append("\r\n │ \t ├ Bound Tokens: ").append(this.tokens.getMap().getObj().size());
    builder.append("\r\n │ \t └ Pending Response Messages: ").append(this.waitingResps.getObj().size());

    builder.append("\r\n ├ Groups");
    builder.append("\r\n │ \t └ Group Map Size: ").append(this.groups.getGroupmap().getObj().size());

    builder.append("\r\n └ Blacklisted IPs");
    if (this.ipBlacklist != null) {
      builder.append("\r\n   \t └ ").append(AppendJsonConverter.convertCollectionStringToJson(this.ipBlacklist.getAll()));
    }

    builder.append("\r\n └ Requests Being Processed: ")
        .append(getCacheFactory().getCache(TioCoreConfigKeys.REQEUST_PROCESSING).keysCollection().size());

    BufferMomeryInfo bufferMomeryInfo = BufferPoolUtils.getBufferMomeryInfo();
    if (bufferMomeryInfo != null) {
      builder.append("\r\n └ BufferMemoryInfo: ");
      builder.append("\r\n   \t ├ Direct Count: ").append(bufferMomeryInfo.directCount);
      builder.append("\r\n   \t ├ Direct Memory Used: ").append(bufferMomeryInfo.directMemoryUsed).append(" bytes");
      builder.append("\r\n   \t ├ Total Capacity: ").append(bufferMomeryInfo.totalCapacity).append(" bytes");

      if (bufferMomeryInfo.responseMemoryStat != null) {
        builder.append("\r\n   \t ├ Response Memory Stat: ").append(formatStat(bufferMomeryInfo.responseMemoryStat));
      }

      if (bufferMomeryInfo.requestBufferMemoryStat != null) {
        builder.append("\r\n   \t ├ Request Buffer Memory Stats:");
        for (BufferMemoryStat stat : bufferMomeryInfo.requestBufferMemoryStat) {
          if (stat != null) {
            builder.append("\r\n   \t │ ").append(formatStat(stat));
          }
        }
      }

      if (bufferMomeryInfo.responseBufferMemoryStat != null) {
        builder.append("\r\n   \t ├ Response Buffer Memory Stats:");
        for (BufferMemoryStat stat : bufferMomeryInfo.responseBufferMemoryStat) {
          if (stat != null) {
            builder.append("\r\n   \t │ ").append(formatStat(stat));
          }
        }
      }
    }

    if (TioServerExecutorService.tioThreadPoolExecutor != null) {
      TioThreadPoolStats stats = TioServerExecutorService.tioThreadPoolExecutor.getStats();

      int poolSize = stats.getPoolSize();
      int active = stats.getActiveCount();

      builder.append("\r\n ├ Thread Pool Statistics");
      builder.append("\r\n │ \t ├ Pool Size / Active / Largest : ").append(poolSize).append(" / ").append(active).append(" / ")
          .append(stats.getLargestPoolSize());
      builder.append("\r\n │ \t ├ Completed Task Count        : ").append(stats.getCompletedTaskCount());
      builder.append("\r\n │ \t ├ Queue Size                  : ").append(stats.getQueueSize());
      builder.append("\r\n │ \t ├ Shutdown / Terminated       : ").append(stats.isShutdown()).append(" / ").append(stats.isTerminated());

      builder.append("\r\n │ \t ├ Submitted / Started / Completed / Failed / Rejected : ").append(stats.getSubmitted()).append(" / ")
          .append(stats.getStarted()).append(" / ").append(stats.getCompleted()).append(" / ").append(stats.getFailed()).append(" / ")
          .append(stats.getRejected());

      builder.append("\r\n │ \t └ AvgQueueMs / AvgExecMs / MaxExecMs : ").append(formatMs(stats.getAvgQueueMs())).append(" / ")
          .append(formatMs(stats.getAvgExecMs())).append(" / ").append(formatMs(stats.getMaxExecMs()));
    }

    log.info(builder.toString());
  }

  private static String formatMs(double v) {
    return String.format("%.3f", v);
  }

  private String formatStat(BufferMemoryStat stat) {
    return String.format("size=%d, newAlloc=%d, cleanCount=%d, reuseHit=%d", stat.bufferSize, stat.statNewAlloc, stat.statCleanCount,
        stat.statReuseHit);
  }

}