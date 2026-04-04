package nexus.io.tio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.enhance.channel.EnhanceAsynchronousChannelProvider;
import nexus.io.enhance.channel.EnhanceAsynchronousServerSocketChannel;
import nexus.io.tio.consts.TioCoreConfigKeys;
import nexus.io.tio.core.Node;
import nexus.io.tio.utils.Threads;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.executor.TioThreadPoolExecutor;
import nexus.io.tio.utils.hutool.StrUtil;

/**
 * @author tanyaowu
 */
public class TioServer {
  private static final Logger log = LoggerFactory.getLogger(TioServer.class);

  public static final String HOTSWAP_WATCH_FILE_ENABLED_KEY = "hotswap.watch.file.enabled";

  private ServerTioConfig serverTioConfig;
  private AsynchronousServerSocketChannel serverSocketChannel;
  private Node serverNode;
  private boolean isWaitingStop = false;
  private static ExecutorService readExecutor;
  private static AsynchronousChannelGroup channelGroup;

  public TioServer(ServerTioConfig serverTioConfig) {
    super();
    this.serverTioConfig = serverTioConfig;
  }

  /**
   * @return the serverTioConfig
   */
  public ServerTioConfig getServerTioConfig() {
    return serverTioConfig;
  }

  /**
   * @return the serverNode
   */
  public Node getServerNode() {
    return serverNode;
  }

  /**
   * @return the serverSocketChannel
   */
  public AsynchronousServerSocketChannel getServerSocketChannel() {
    return serverSocketChannel;
  }

  /**
   * @return the isWaitingStop
   */
  public boolean isWaitingStop() {
    return isWaitingStop;
  }

  /**
   * @param serverTioConfig the serverTioConfig to set
   */
  public void setServerTioConfig(ServerTioConfig serverTioConfig) {
    this.serverTioConfig = serverTioConfig;
  }

  /**
   * @param isWaitingStop the isWaitingStop to set
   */
  public void setWaitingStop(boolean isWaitingStop) {
    this.isWaitingStop = isWaitingStop;
  }

  public void start(String serverIp, int serverPort) throws IOException {
    serverTioConfig.init();
    serverTioConfig.getCacheFactory().register(TioCoreConfigKeys.REQEUST_PROCESSING, null, null, null);

    this.serverNode = new Node(serverIp, serverPort);
    if (EnvUtils.getBoolean("tio.core.hotswap.reload", false)) {
      readExecutor = Threads.getReadExecutor();
      channelGroup = AsynchronousChannelGroup.withThreadPool(readExecutor);
      serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
    } else {
      // serverSocketChannel = AsynchronousServerSocketChannel.open();
      readExecutor = serverTioConfig.getWorkderExecutor();
      int workerThreads = serverTioConfig.getWorkerThreads();
      log.info("{} worker threads:{}", serverTioConfig.getName(), workerThreads);

      if (readExecutor == null) {
        ThreadFactory threadFactory = serverTioConfig.getWorkThreadFactory();
        if (threadFactory == null) {
          AtomicInteger threadNumber = new AtomicInteger(1);
          threadFactory = r -> new Thread(r, "t-io-" + threadNumber.getAndIncrement());
        }

        TioThreadPoolExecutor tioThreadPoolExecutor = new TioThreadPoolExecutor(workerThreads, workerThreads, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(workerThreads), threadFactory);

        TioServerExecutorService.tioThreadPoolExecutor = tioThreadPoolExecutor;
        readExecutor = tioThreadPoolExecutor;
      }
      EnhanceAsynchronousChannelProvider provider = new EnhanceAsynchronousChannelProvider(false);
      channelGroup = provider.openAsynchronousChannelGroup(readExecutor, workerThreads);

      // 使用提供者创建服务器通道
      AsynchronousServerSocketChannel openAsynchronousServerSocketChannel = provider
          .openAsynchronousServerSocketChannel(channelGroup);
      serverSocketChannel = (EnhanceAsynchronousServerSocketChannel) openAsynchronousServerSocketChannel;
    }

    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);

    InetSocketAddress listenAddress = null;

    if (StrUtil.isBlank(serverIp)) {
      listenAddress = new InetSocketAddress(serverPort);
    } else {
      listenAddress = new InetSocketAddress(serverIp, serverPort);
    }

    serverSocketChannel.bind(listenAddress, serverTioConfig.getBacklog());

    AcceptCompletionHandler acceptCompletionHandler = new AcceptCompletionHandler();
    serverSocketChannel.accept(this, acceptCompletionHandler);

    serverTioConfig.startTime = System.currentTimeMillis();
    Threads.getTioExecutor();
  }

  /**
   * 
   * @return`
   * 
   * @author tanyaowu
   */
  public boolean stop() {
    isWaitingStop = true;

    if (channelGroup != null && !channelGroup.isShutdown()) {
      try {
        channelGroup.shutdownNow();
        if (!channelGroup.awaitTermination(5, TimeUnit.SECONDS)) {
          log.warn("channelGroup did not terminate within the timeout");
        }
      } catch (Exception e) {
        log.error("Faild to execute channelGroup.shutdownNow()", e);
      }
    }

    if (readExecutor != null && !readExecutor.isShutdown()) {
      try {
        readExecutor.shutdownNow();
        if (!readExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          log.warn("groupExecutor did not terminate within the timeout");
        }
      } catch (Exception e) {
        log.error("Failed to close groupExecutor", e);
      }
    }

    if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
      try {
        serverSocketChannel.close();
      } catch (Exception e) {
        log.error("Failed to close serverSocketChannel", e);
      }
    }
    log.info(this.serverNode + " stopped");
    
    boolean ret = false;
    serverTioConfig.setStopped(true);
    if (EnvUtils.getBoolean(HOTSWAP_WATCH_FILE_ENABLED_KEY, true)) {
      ret = Threads.close();
    }
    return ret;
  }
}
