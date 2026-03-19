package com.litongjava.tio.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.aio.PacketMeta;
import com.litongjava.tio.core.ssl.SslFacadeContext;
import com.litongjava.tio.core.stat.ChannelStat;
import com.litongjava.tio.core.stat.IpStat;
import com.litongjava.tio.utils.hutool.CollUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lock.SetWithLock;
import com.litongjava.tio.utils.prop.MapWithLockPropSupport;

/**
 * @author tanyaowu 2017年10月19日 上午9:39:46
 */
public abstract class ChannelContext extends MapWithLockPropSupport {
  private static final Logger log = LoggerFactory.getLogger(ChannelContext.class);
  private static final String DEFAULT_ATTUBITE_KEY = "t-io-d-a-k";
  public static final String UNKNOWN_ADDRESS_IP = "$UNKNOWN";
  public static final AtomicInteger UNKNOWN_ADDRESS_PORT_SEQ = new AtomicInteger();
  public boolean isReconnect = false;

  public boolean isBind = false;
  /**
   * 解码出现异常时，是否打印异常日志 此值默认与TioConfig.logWhenDecodeError保持一致
   */
  public boolean logWhenDecodeError = false;
  /**
   * 此值不设时，心跳时间取TioConfig.heartbeatTimeout
   * 当然这个值如果小于TioConfig.heartbeatTimeout，定时检查的时间间隔还是以TioConfig.heartbeatTimeout为准，只是在判断时用此值
   */
  public Long heartbeatTimeout = null;
  /**
   * 一个packet所需要的字节数（用于应用告诉框架，下一次解码所需要的字节长度，省去冗余解码带来的性能损耗）
   */
  public Integer packetNeededLength = null;
  public TioConfig tioConfig = null;
  public final ReentrantReadWriteLock closeLock = new ReentrantReadWriteLock();

  public SslFacadeContext sslFacadeContext;
  public String userId;
  private String token;
  private String bsId;
  private Long dataId;
  private Boolean dataBool;
  public boolean isWaitingClose = false;
  public boolean isClosed = true;
  public boolean isRemoved = false;
  public boolean isVirtual = false;
  public boolean hasTempDir = false;
  public final ChannelStat stat = new ChannelStat();
  /** The asynchronous socket channel. */
  public AsynchronousSocketChannel asynchronousSocketChannel;
  private String id = null;
  private Node clientNode;
  private Node proxyClientNode = null; // 一些连接是代理的，譬如web服务器放在nginx后面，此时需要知道最原始的ip
  private Node serverNode;
  /**
   * 该连接在哪些组中
   */
  public SetWithLock<String> groups;
  private Integer readBufferSize = null; // 个性化readBufferSize
  public CloseMeta closeMeta = new CloseMeta();
  private ChannelCloseCode closeCode = ChannelCloseCode.INIT_STATUS; // 连接关闭的原因码

  // 添加发送队列和控制变量
  public final Queue<Packet> sendQueue = new ConcurrentLinkedQueue<>();
  public final AtomicBoolean isSending = new AtomicBoolean(false);

  /**
   *
   * @param tioConfig
   * @param asynchronousSocketChannel
   * @author tanyaowu
   */
  public ChannelContext(TioConfig tioConfig, AsynchronousSocketChannel asynchronousSocketChannel) {
    super();
    init(tioConfig, asynchronousSocketChannel);

    if (tioConfig.sslConfig != null) {
      try {
        SslFacadeContext sslFacadeContext = new SslFacadeContext(this);
        if (tioConfig.isServer()) {
          sslFacadeContext.beginHandshake();
        }
      } catch (Exception e) {
        log.error("在开始SSL握手时发生了异常", e);
        Tio.close(this, "在开始SSL握手时发生了异常" + e.getMessage(), ChannelCloseCode.SSL_ERROR_ON_HANDSHAKE);
        return;
      }
    }
  }

  public ChannelContext(TioConfig tioConfig, AsynchronousSocketChannel asynchronousSocketChannel, String ip, int port) {
    super();
    init(tioConfig, asynchronousSocketChannel, ip, port);

    if (tioConfig.sslConfig != null) {
      try {
        SslFacadeContext sslFacadeContext = new SslFacadeContext(this);
        if (tioConfig.isServer()) {
          sslFacadeContext.beginHandshake();
        }
      } catch (Exception e) {
        log.error("在开始SSL握手时发生了异常", e);
        Tio.close(this, "在开始SSL握手时发生了异常" + e.getMessage(), ChannelCloseCode.SSL_ERROR_ON_HANDSHAKE);
        return;
      }
    }
  }

  /**
   * 创建一个虚拟ChannelContext，主要用来模拟一些操作，譬如压力测试，真实场景中用得少
   * 
   * @param tioConfig
   */
  public ChannelContext(TioConfig tioConfig) {
    this(tioConfig, tioConfig.getTioUuid().id());
  }

  /**
   * 创建一个虚拟ChannelContext，主要用来模拟一些操作，譬如压力测试，真实场景中用得少
   * 
   * @param tioConfig
   * @param id        ChannelContext id
   * @author tanyaowu
   */
  public ChannelContext(TioConfig tioConfig, String id) {
    isVirtual = true;
    this.tioConfig = tioConfig;
    Node clientNode = new Node("127.0.0.1", 26254);
    this.clientNode = clientNode;
    this.id = id;// tioConfig.getTioUuid().uuid();
    if (StrUtil.isBlank(id)) {
      this.id = tioConfig.getTioUuid().id();
    }
  }

  private void assignAnUnknownClientNode() {
    Node clientNode = new Node(UNKNOWN_ADDRESS_IP, UNKNOWN_ADDRESS_PORT_SEQ.incrementAndGet());
    setClientNode(clientNode);
  }

  public Node createClientNode(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException {
    InetSocketAddress inetSocketAddress = (InetSocketAddress) asynchronousSocketChannel.getRemoteAddress();
    Node clientNode = new Node(inetSocketAddress.getHostString(), inetSocketAddress.getPort());
    return clientNode;
  }

  public Node createClientNode(String clientIp, int port) {
    return new Node(clientIp, port);
  }

  /**
   *
   * @param obj
   * @return
   * @author tanyaowu
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ChannelContext other = (ChannelContext) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  /**
   * 等价于：getAttribute(DEFAULT_ATTUBITE_KEY)
   * 
   * @deprecated 建议使用get()
   * @return
   */
  public Object getAttribute() {
    return get();
  }

  /**
   * 等价于：getAttribute(DEFAULT_ATTUBITE_KEY)<br>
   * 等价于：getAttribute()<br>
   * 
   * @return
   */
  public Object get() {
    return get(DEFAULT_ATTUBITE_KEY);
  }

  /**
   * @return the remoteNode
   */
  public Node getClientNode() {
    return clientNode;
  }

  public SetWithLock<String> getGroups() {
    return groups;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the serverNode
   */
  public Node getServerNode() {
    return serverNode;
  }

  public String getToken() {
    return token;
  }

  /**
   *
   * @return
   * @author tanyaowu
   */
  @Override
  public int hashCode() {
    if (StrUtil.isNotBlank(id)) {
      return this.id.hashCode();
    } else {
      return super.hashCode();
    }
  }

  public void init(TioConfig tioConfig, AsynchronousSocketChannel asynchronousSocketChannel) {
    id = tioConfig.getTioUuid().id();
    this.setTioConfig(tioConfig);
    this.setAsynchronousSocketChannel(asynchronousSocketChannel);
    this.logWhenDecodeError = tioConfig.logWhenDecodeError;
  }

  public void init(TioConfig tioConfig, AsynchronousSocketChannel asynchronousSocketChannel, String clientIp,
      int port) {
    id = tioConfig.getTioUuid().id();
    this.setTioConfig(tioConfig);
    this.setAsynchronousSocketChannel(asynchronousSocketChannel, clientIp, port);
    this.logWhenDecodeError = tioConfig.logWhenDecodeError;
  }

  /**
   * 
   * @param packet
   * @param isSentSuccess
   * @author tanyaowu
   */
  public void processAfterSent(Packet packet, Boolean isSentSuccess) {
    isSentSuccess = isSentSuccess == null ? false : isSentSuccess;
    PacketMeta meta = packet.getMeta();
    if (meta != null) {
      CountDownLatch countDownLatch = meta.getCountDownLatch();
      // traceBlockPacket(SynPacketAction.BEFORE_DOWN, packet, countDownLatch, null);
      countDownLatch.countDown();
    }

    try {
      if (log.isDebugEnabled()) {
        log.debug("{} Sent {}", this, packet.logstr());
      }

      // 非SSL or SSL已经握手
      if (this.sslFacadeContext == null || this.sslFacadeContext.isHandshakeCompleted()) {
        if (tioConfig.getAioListener() != null) {
          try {
            tioConfig.getAioListener().onAfterSent(this, packet, isSentSuccess);
          } catch (Exception e) {
            log.error(e.toString(), e);
          }
        }

        if (tioConfig.statOn) {
          tioConfig.groupStat.sentPackets.incrementAndGet();
          stat.sentPackets.incrementAndGet();
        }

        if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
          try {
            for (Long v : tioConfig.ipStats.durationList) {
              IpStat ipStat = tioConfig.ipStats.get(v, this);
              ipStat.getSentPackets().incrementAndGet();
              tioConfig.getIpStatListener().onAfterSent(this, packet, isSentSuccess, ipStat);
            }
          } catch (Exception e) {
            log.error(e.toString(), e);
          }
        }
      }
    } catch (Throwable e) {
      log.error(e.toString(), e);
    }
  }

  /**
   * @param asynchronousSocketChannel the asynchronousSocketChannel to set
   */
  public void setAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
    this.asynchronousSocketChannel = asynchronousSocketChannel;

    if (asynchronousSocketChannel != null) {
      try {
        Node clientNode = createClientNode(asynchronousSocketChannel);
        setClientNode(clientNode);
      } catch (IOException e) {
        log.error(e.getMessage());
        assignAnUnknownClientNode();
      }
    } else {
      log.error("assignAnUnknownClientNode:{}", asynchronousSocketChannel);
      assignAnUnknownClientNode();
    }
  }

  private void setAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel, String clientIp,
      int port) {
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    Node clientNode = createClientNode(clientIp, port);
    setClientNode(clientNode);

  }

  /**
   * 等价于：setAttribute(DEFAULT_ATTUBITE_KEY, value)<br>
   * 仅仅是为了内部方便，不建议大家使用<br>
   * 
   * @deprecated 不建议各位同学使用这个方法，建议使用set("name1", object1)
   * @param value
   * @author tanyaowu
   */
  public void setAttribute(Object value) {
    set(value);
  }

  /**
   * 等价于：set(DEFAULT_ATTUBITE_KEY, value)<br>
   * 等价于：setAttribute(Object value)<br>
   * 
   * @deprecated 不建议各位同学使用这个方法，建议使用set("name1", object1)
   * @param value
   */
  public void set(Object value) {
    set(DEFAULT_ATTUBITE_KEY, value);
  }

  /**
   * @param remoteNode the remoteNode to set
   */
  public void setClientNode(Node clientNode) {
    if (!this.tioConfig.isShortConnection) {
      if (this.clientNode != null) {
        tioConfig.clientNodes.remove(this);
      }
    }

    this.clientNode = clientNode;
    if (this.tioConfig.isShortConnection) {
      return;
    }

    if (this.clientNode != null && !Objects.equals(UNKNOWN_ADDRESS_IP, this.clientNode.getHost())) {
      tioConfig.clientNodes.put(this);
    }
  }

  /**
   * @param isClosed the isClosed to set
   */
  public void setClosed(boolean isClosed) {
    this.isClosed = isClosed;
    if (isClosed) {
      if (clientNode == null || !UNKNOWN_ADDRESS_IP.equals(clientNode.getHost())) {
        assignAnUnknownClientNode();
      }
    }
  }

  /**
   * @param tioConfig the tioConfig to set
   */
  public void setTioConfig(TioConfig tioConfig) {
    this.tioConfig = tioConfig;

    if (tioConfig != null) {
      tioConfig.connections.add(this);
    }
  }

  public void setPacketNeededLength(Integer packetNeededLength) {
    this.packetNeededLength = packetNeededLength;
  }

  public void setReconnect(boolean isReconnect) {
    this.isReconnect = isReconnect;
  }

  /**
   * @param isRemoved the isRemoved to set
   */
  public void setRemoved(boolean isRemoved) {
    this.isRemoved = isRemoved;
  }

  /**
   * @param serverNode the serverNode to set
   */
  public void setServerNode(Node serverNode) {
    this.serverNode = serverNode;
  }

  public void setSslFacadeContext(SslFacadeContext sslFacadeContext) {
    this.sslFacadeContext = sslFacadeContext;
  }

  public void setToken(String token) {
    this.token = token;
  }

  /**
   * @param userId the userid to set 给框架内部用的，用户请勿调用此方法
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    if (serverNode != null) {
      sb.append("server:").append(serverNode.toString());
    } else {
      sb.append("server:").append("NULL");
    }
    if (clientNode != null) {
      sb.append(", client:").append(clientNode.toString());
    } else {
      sb.append(", client:").append("NULL");
    }

    if (isVirtual) {
      sb.append(", virtual");
    }

    return sb.toString();
  }

  /**
   * @return the bsId
   */
  public String getBsId() {
    return bsId;
  }

  /**
   * @param bsId the bsId to set
   */
  public void setBsId(String bsId) {
    this.bsId = bsId;
  }

  public TioConfig getTioConfig() {
    return tioConfig;
  }

  /**
   * 是否是服务器端
   * 
   * @return
   * @author tanyaowu
   */
  public abstract boolean isServer();

  /**
   * @return the heartbeatTimeout
   */
  public Long getHeartbeatTimeout() {
    return heartbeatTimeout;
  }

  /**
   * @param heartbeatTimeout the heartbeatTimeout to set
   */
  public void setHeartbeatTimeout(Long heartbeatTimeout) {
    this.heartbeatTimeout = heartbeatTimeout;
  }

  public Integer getReadBufferSize() {
    if (readBufferSize != null && readBufferSize > 0) {
      return readBufferSize;
    }
    return this.tioConfig.getReadBufferSize();
  }

  public void setReadBufferSize(Integer readBufferSize) {
    this.readBufferSize = Math.min(readBufferSize, TcpConst.MAX_DATA_LENGTH);
  }

  /**
   * @return the proxyClientNode
   */
  public Node getProxyClientNode() {
    return proxyClientNode;
  }

  private void swithIpStat(IpStat oldIpStat, IpStat newIpStat, ChannelStat myStat) {
    oldIpStat.getHandledBytes().addAndGet(-myStat.getHandledBytes().get());
    oldIpStat.getHandledPacketCosts().addAndGet(-myStat.getHandledPacketCosts().get());
    oldIpStat.getHandledPackets().addAndGet(-myStat.getHandledPackets().get());
    oldIpStat.getReceivedBytes().addAndGet(-myStat.getReceivedBytes().get());
    oldIpStat.getReceivedPackets().addAndGet(-myStat.getReceivedPackets().get());
    oldIpStat.getReceivedTcps().addAndGet(-myStat.getReceivedTcps().get());
    oldIpStat.getRequestCount().addAndGet(-1);
    oldIpStat.getSentBytes().addAndGet(-myStat.getSentBytes().get());
    oldIpStat.getSentPackets().addAndGet(-myStat.getSentPackets().get());
    oldIpStat.getStart();

    newIpStat.getHandledBytes().addAndGet(myStat.getHandledBytes().get());
    newIpStat.getHandledPacketCosts().addAndGet(myStat.getHandledPacketCosts().get());
    newIpStat.getHandledPackets().addAndGet(myStat.getHandledPackets().get());
    newIpStat.getReceivedBytes().addAndGet(myStat.getReceivedBytes().get());
    newIpStat.getReceivedPackets().addAndGet(myStat.getReceivedPackets().get());
    newIpStat.getReceivedTcps().addAndGet(myStat.getReceivedTcps().get());
    newIpStat.getRequestCount().addAndGet(1);
    newIpStat.getSentBytes().addAndGet(myStat.getSentBytes().get());
    newIpStat.getSentPackets().addAndGet(myStat.getSentPackets().get());
    newIpStat.getStart();
  }

  /**
   * @param proxyClientNode the proxyClientNode to set
   */
  public void setProxyClientNode(Node proxyClientNode) {
    this.proxyClientNode = proxyClientNode;
    if (proxyClientNode != null) {
      // 将性能数据进行转移
      if (!Objects.equals(proxyClientNode.getHost(), clientNode.getHost())) {

        if (CollUtil.isNotEmpty(tioConfig.ipStats.durationList)) {
          try {
            for (Long v : tioConfig.ipStats.durationList) {
              IpStat oldIpStat = (IpStat) tioConfig.ipStats._get(v, this, true, false);
              IpStat newIpStat = (IpStat) tioConfig.ipStats.get(v, this);
              ChannelStat myStat = this.stat;
              swithIpStat(oldIpStat, newIpStat, myStat);
            }
          } catch (Exception e) {
            log.error(e.toString(), e);
          }
        }
      }
    }
  }

  public ChannelCloseCode getCloseCode() {
    return closeCode;
  }

  public void setCloseCode(ChannelCloseCode closeCode) {
    this.closeCode = closeCode;
  }

  /**
   * @author tanyaowu
   */
  public static class CloseMeta {
    public Throwable throwable;
    public String remark;
    public boolean isNeedRemove;

    public Throwable getThrowable() {
      return throwable;
    }

    public void setThrowable(Throwable throwable) {
      this.throwable = throwable;
    }

    public String getRemark() {
      return remark;
    }

    public void setRemark(String remark) {
      this.remark = remark;
    }

    public boolean isNeedRemove() {
      return isNeedRemove;
    }

    public void setNeedRemove(boolean isNeedRemove) {
      this.isNeedRemove = isNeedRemove;
    }
  }

  public String getClientIpAndPort() {
    Node client = this.getProxyClientNode();
    if (client == null) {
      client = this.getClientNode();
    }

    String ip = client.getHost();
    int port = client.getPort();
    return ip + ':' + port;
  }

  public Long getDataId() {
    return dataId;
  }

  public void setDataId(Long dataId) {
    this.dataId = dataId;
  }

  public Boolean getDataBool() {
    return dataBool;
  }

  public void setDataBool(Boolean dataBool) {
    this.dataBool = dataBool;
  }
}
