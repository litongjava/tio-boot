package nexus.io.tio.core;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.aio.AioId;
import nexus.io.aio.Packet;
import nexus.io.constants.ServerConfigKeys;
import nexus.io.tio.client.ClientTioConfig;
import nexus.io.tio.consts.TioConst;
import nexus.io.tio.consts.TioCoreConfigKeys;
import nexus.io.tio.core.cache.IpStatMapCacheRemovalListener;
import nexus.io.tio.core.intf.AioHandler;
import nexus.io.tio.core.intf.AioListener;
import nexus.io.tio.core.intf.GroupListener;
import nexus.io.tio.core.maintain.BsIds;
import nexus.io.tio.core.maintain.ClientNodes;
import nexus.io.tio.core.maintain.Groups;
import nexus.io.tio.core.maintain.Ids;
import nexus.io.tio.core.maintain.IpBlacklist;
import nexus.io.tio.core.maintain.IpStats;
import nexus.io.tio.core.maintain.Ips;
import nexus.io.tio.core.maintain.Tokens;
import nexus.io.tio.core.maintain.Users;
import nexus.io.tio.core.ssl.SslConfig;
import nexus.io.tio.core.stat.DefaultIpStatListener;
import nexus.io.tio.core.stat.GroupStat;
import nexus.io.tio.core.stat.IpStatListener;
import nexus.io.tio.server.ServerTioConfig;
import nexus.io.tio.utils.SystemTimer;
import nexus.io.tio.utils.cache.CacheFactory;
import nexus.io.tio.utils.cache.RemovalListenerWrapper;
import nexus.io.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.lock.MapWithLock;
import nexus.io.tio.utils.lock.SetWithLock;
import nexus.io.tio.utils.prop.MapWithLockPropSupport;

/**
 * 
 * @author tanyaowu 2016年10月10日 下午5:25:43
 */
public abstract class TioConfig extends MapWithLockPropSupport {
  private static final Logger log = LoggerFactory.getLogger(TioConfig.class);
  /**
   * 默认的接收数据的buffer size
   */
  public static final int READ_BUFFER_SIZE = EnvUtils.getInt(TioCoreConfigKeys.TIO_DEFAULT_READ_BUFFER_SIZE, 8192);
  public static final int WRITE_CHUNK_SIZE = EnvUtils.getInt(TioCoreConfigKeys.TIO_DEFAULT_WRITE_CHUNK_SIZE, 8192);
  /** 是否使用直接内存缓冲区（可通过环境变量开关） */
  public static final boolean direct = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_BUFFER_DIRECT, true);

  public static final boolean disgnostic = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_DIAGNOSTIC);
  public static final boolean printStats = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_STATS_PRINT);

  public static final int WORK_THREAD_FACTOR = EnvUtils.getInt(ServerConfigKeys.SERVER_WORK_THREAD_FACTOR, 2);
  public static final int cpuNum = Runtime.getRuntime().availableProcessors();
  private int workerThreads = EnvUtils.getInt(TioCoreConfigKeys.TIO_CORE_THREADS, cpuNum * WORK_THREAD_FACTOR);

  private ThreadFactory workThreadFactory;
  private ExecutorService bizExecutor;
  private ExecutorService workderExecutor;

  /**
   * 本jvm中所有的ServerTioConfig对象
   */
  public static final Set<ServerTioConfig> ALL_SERVER_GROUPCONTEXTS = new HashSet<>();
  /**
   * 本jvm中所有的ClientTioConfig对象
   */
  public static final Set<ClientTioConfig> ALL_CLIENT_GROUPCONTEXTS = new HashSet<>();
  /**
   * 本jvm中所有的TioConfig对象
   */
  public static final Set<TioConfig> ALL_GROUPCONTEXTS = new HashSet<>();

  private final static AtomicInteger ID_ATOMIC = new AtomicInteger();
  private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
  public boolean isShortConnection = false;
  public SslConfig sslConfig = null;

  public GroupStat groupStat = null;
  public boolean statOn = true;

  public boolean checkAttacks = true;
  public boolean ignoreDecodeFail = false;
  public boolean runOnAndroid = false;
  public PacketConverter packetConverter = null;

  private String charset = TioConst.UTF_8;

  /**
   * 缓存工厂
   */
  private CacheFactory cacheFactory;

  /**
   * 移除IP监听
   */
  @SuppressWarnings("rawtypes")
  private RemovalListenerWrapper ipRemovalListenerWrapper;
  /**
   * 启动时间
   */
  public long startTime = SystemTimer.currTime;

  /**
   * 心跳超时时间(单位: 毫秒)，如果用户不希望框架层面做心跳相关工作，请把此值设为0或负数
   */
  public long heartbeatTimeout = 1000 * 120;
  /**
   * 解码出现异常时，是否打印异常日志
   */
  public boolean logWhenDecodeError = false;

  /**
   * 接收数据的buffer size
   */
  private int readBufferSize = READ_BUFFER_SIZE;
  private GroupListener groupListener = null;
  private AioId tioUuid = new DefaultTAioId();
  public ClientNodes clientNodes = new ClientNodes();
  public SetWithLock<ChannelContext> connections = new SetWithLock<ChannelContext>(new HashSet<ChannelContext>());
  public Groups groups = new Groups();
  public Users users = new Users();
  public Tokens tokens = new Tokens();
  public Ids ids = new Ids();
  public BsIds bsIds = new BsIds();
  public Ips ips = new Ips();
  public IpStats ipStats = new IpStats(this, null);;
  protected String id;
  /**
   * 解码异常多少次就把ip拉黑
   */
  protected int maxDecodeErrorCountForIp = 10;
  protected String name = "Untitled";
  private IpStatListener ipStatListener = DefaultIpStatListener.me;
  private boolean isStopped = false;
  /**
   * ip黑名单
   */
  public IpBlacklist ipBlacklist = null;
  public MapWithLock<Integer, Packet> waitingResps = new MapWithLock<Integer, Packet>(new HashMap<Integer, Packet>());

  public TioConfig() {

  }

  public TioConfig(CacheFactory cacheFactory) {
    this.cacheFactory = cacheFactory;
  }

  public TioConfig(CacheFactory cacheFactory, RemovalListenerWrapper<?> ipRemovalListenerWrapper) {
    this.cacheFactory = cacheFactory;
    this.ipRemovalListenerWrapper = ipRemovalListenerWrapper;
  }

  public TioConfig(String name) {
    this.name = name;
    try {
      // Android 上会有这个类
      Class.forName("android.os.Build");
      runOnAndroid = true;
    } catch (ClassNotFoundException ignored) {
    }
  }

  /**
   * 获取AioHandler对象
   * 
   * @return
   */
  public abstract AioHandler getAioHandler();

  /**
   * 获取AioListener对象
   */
  public abstract AioListener getAioListener();

  public ByteOrder getByteOrder() {
    return byteOrder;
  }

  /**
   * @return the groupListener
   */
  public GroupListener getGroupListener() {
    return groupListener;
  }

  public String getId() {
    return id;
  }

  /**
   * @return the tioUuid
   */
  public AioId getTioUuid() {
    return tioUuid;
  }

  /**
   * @return the syns
   */
  public MapWithLock<Integer, Packet> getWaitingResps() {
    return waitingResps;
  }

  /**
   * @return the isStop
   */
  public boolean isStopped() {
    return isStopped;
  }

  /**
   *
   * @param byteOrder
   * @author tanyaowu
   */
  public void setByteOrder(ByteOrder byteOrder) {
    this.byteOrder = byteOrder;
  }

  /**
   * @param groupListener the groupListener to set
   */
  public void setGroupListener(GroupListener groupListener) {
    this.groupListener = groupListener;
  }

  /**
   * @param heartbeatTimeout the heartbeatTimeout to set
   */
  public void setHeartbeatTimeout(long heartbeatTimeout) {
    this.heartbeatTimeout = heartbeatTimeout;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * @param readBufferSize the readBufferSize to set
   */
  public void setReadBufferSize(int readBufferSize) {
    this.readBufferSize = Math.min(readBufferSize, TcpConst.MAX_DATA_LENGTH);
  }

  /**
   * @param isShortConnection the isShortConnection to set
   */
  public void setShortConnection(boolean isShortConnection) {
    this.isShortConnection = isShortConnection;
  }

  /**
   * @param isStop the isStop to set
   */
  public void setStopped(boolean isStopped) {
    this.isStopped = isStopped;
  }

  /**
   * @param tioUuid the tioUuid to set
   */
  public void setTioUuid(AioId tioUuid) {
    this.tioUuid = tioUuid;
  }

  public void setSslConfig(SslConfig sslConfig) {
    this.sslConfig = sslConfig;
  }

  public IpStatListener getIpStatListener() {
    return ipStatListener;
  }

  public void setIpStatListener(IpStatListener ipStatListener) {
    this.ipStatListener = ipStatListener;
    setDefaultIpRemovalListenerWrapper();
  }

  public GroupStat getGroupStat() {
    return groupStat;
  }

  /**
   * 是服务器端还是客户端
   * 
   * @return
   * @author tanyaowu
   */
  public abstract boolean isServer();

  public int getReadBufferSize() {
    return readBufferSize;
  }

  public boolean isSsl() {
    return sslConfig != null;
  }

  public void setCacheFactory(CacheFactory cacheFactory) {
    this.cacheFactory = cacheFactory;
  }

  public CacheFactory getCacheFactory() {
    return cacheFactory;
  }

  public void setIpRemovalListenerWrapper(RemovalListenerWrapper<?> ipRemovalListenerWrapper) {
    this.ipRemovalListenerWrapper = ipRemovalListenerWrapper;
  }

  public RemovalListenerWrapper<?> getIpRemovalListenerWrapper() {
    return ipRemovalListenerWrapper;
  }

  /**
   * @return the charset
   */
  public String getCharset() {
    return charset;
  }

  /**
   * @param charset the charset to set
   */
  public void setCharset(String charset) {
    this.charset = charset;
  }

  public int getWorkerThreads() {
    return workerThreads;
  }

  public void setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
  }

  public ThreadFactory getWorkThreadFactory() {
    return workThreadFactory;
  }

  public void setWorkThreadFactory(ThreadFactory threadFactory) {
    this.workThreadFactory = threadFactory;
  }

  public ExecutorService getBizExecutor() {
    return bizExecutor;
  }

  public void setBizExecutor(ExecutorService e) {
    this.bizExecutor = e;
  }

  public ExecutorService getWorkderExecutor() {
    return workderExecutor;
  }

  public void setWorkderExecutor(ExecutorService workderExecutor) {
    this.workderExecutor = workderExecutor;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void setDefaultIpRemovalListenerWrapper() {
    this.ipRemovalListenerWrapper = new RemovalListenerWrapper();
    IpStatMapCacheRemovalListener ipStatMapCacheRemovalListener = new IpStatMapCacheRemovalListener(this,
        ipStatListener);
    ipRemovalListenerWrapper.setListener(ipStatMapCacheRemovalListener);
  }

  public void init() {
    if (cacheFactory == null) {
      // mapCacheFactory
      this.cacheFactory = ConcurrentMapCacheFactory.INSTANCE;
    }

    if (ipRemovalListenerWrapper == null) {
      setDefaultIpRemovalListenerWrapper();
    }

    ALL_GROUPCONTEXTS.add(this);
    if (this instanceof ServerTioConfig) {
      ALL_SERVER_GROUPCONTEXTS.add((ServerTioConfig) this);
    } else {
      ALL_CLIENT_GROUPCONTEXTS.add((ClientTioConfig) this);
    }

    if (ALL_GROUPCONTEXTS.size() > 20) {
      log.warn("You have created {} TioConfig objects, you might be misusing t-io.", ALL_GROUPCONTEXTS.size());
    }
    this.id = ID_ATOMIC.incrementAndGet() + "";

    if (this.ipStats == null) {
      this.ipStats = new IpStats(this, null);
    }
  }

}
