package com.litongjava.tio.boot.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.boot.cache.StaticResourcesCache;
import com.litongjava.tio.utils.cache.AbsCache;

/**
 * 使用 JDK1.8 自带的 WatchService 来监控文件/目录变更，替代 Commons-IO 的 FileAlterationMonitor 等。
 * 
 * 用法示例：
 *   DirectoryWatcher watcher = new DirectoryWatcher(Paths.get(httpConfig.getPageRoot()), this);
 *   watcher.start(); // 启动后台线程开始监听
 * 
 * 需要在停机时调用 watcher.stop();
 */
public class DirectoryWatcher {
  private static final Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);

  /** JDK WatchService */
  private final WatchService watchService;
  /** 根目录的 Path */
  private final Path rootDir;
  /** 用于存放每个注册的 WatchKey 对应的 Path */
  private final Map<WatchKey, Path> keyPathMap = new HashMap<>();
  /** 后台线程标志位 */
  private volatile boolean running = false;
  /** 后台线程本身 */
  private Thread watcherThread;

  /**
   * 构造函数：传入需要监听的根目录 Path，以及发生变更时调用 removeCache 的 dispatcher。
   *
   * @param rootDir      待监听的根目录
   * @param dispatcher   有 removeCache(File) 方法的对象
   * @throws IOException 如果 WatchService 创建失败
   */
  public DirectoryWatcher(Path rootDir) throws IOException {
    this.rootDir = rootDir;
    this.watchService = FileSystems.getDefault().newWatchService();
  }

  /**
   * 启动后台线程并注册根目录及其子目录。
   */
  public void start() throws IOException {
    if (running) {
      return;
    }
    // 递归注册根目录及所有子目录
    registerAll(rootDir);

    running = true;
    watcherThread = new Thread(this::processEvents, "DirectoryWatcher-Thread");
    watcherThread.setDaemon(true);
    watcherThread.start();
    log.info("DirectoryWatcher started on {}", rootDir);
  }

  /**
   * 停止监听，并关闭 WatchService。
   */
  public void stop() {
    running = false;
    if (watcherThread != null) {
      watcherThread.interrupt();
    }
    try {
      watchService.close();
    } catch (IOException e) {
      log.error("Error closing WatchService", e);
    }
    log.info("DirectoryWatcher stopped on {}", rootDir);
  }

  /**
   * 递归地将指定目录及其所有子目录注册到 WatchService。
   *
   * @param startDir  要注册的起始目录
   * @throws IOException
   */
  private void registerAll(final Path startDir) throws IOException {
    // Files.walk(startDir) 也可以，但这里用显式递归，兼容性更好
    Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * 将单个目录注册到 WatchService 中，监听 CREATE / MODIFY / DELETE 三种事件。
   *
   * @param dir  要注册的目录
   * @throws IOException
   */
  private void register(Path dir) throws IOException {
    WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
    keyPathMap.put(key, dir);
    log.debug("Registered directory to WatchService: {}", dir);
  }

  /**
   * 后台线程的核心逻辑：不断从 watchService 中拉取事件，分发处理。
   */
  private void processEvents() {
    while (running) {
      WatchKey key;
      try {
        // 阻塞式获取下一组事件，可改成 poll(XXX, TimeUnit) 
        key = watchService.take();
      } catch (InterruptedException e) {
        // 被中断，退出循环
        Thread.currentThread().interrupt();
        break;
      } catch (ClosedWatchServiceException e) {
        // 已关闭，退出循环
        break;
      }

      Path dir = keyPathMap.get(key);
      if (dir == null) {
        // 未知 key，跳过
        key.reset();
        continue;
      }

      // 遍历此 WatchKey 上所有的事件
      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        // OVERFLOW 事件意味着有些事件被丢弃了，我们可以选择忽略或记录
        if (kind == StandardWatchEventKinds.OVERFLOW) {
          log.warn("Overflow event occurred in directory watcher: {}", dir);
          continue;
        }

        // 获取相对路径（相对于 dir）
        @SuppressWarnings("unchecked")
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path name = ev.context();
        Path child = dir.resolve(name); // 得到文件/目录的绝对路径

        // 如果是目录创建事件（ENTRY_CREATE 且 child 是目录），则递归注册新目录
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
          try {
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
              registerAll(child);
            }
          } catch (IOException e) {
            log.error("Error registering newly created subdirectory: {}", child, e);
          }
        }

        // 下面把事件分发给 dispatcher.removeCache(File)
        File changedFile = child.toFile();
        // 只要是文件或目录的创建、修改、删除，都调用 removeCache
        if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY || kind == StandardWatchEventKinds.ENTRY_DELETE) {
          log.info("File system event [{}] on: {}", kind.name(), changedFile);
          try {
            AbsCache staticResCache = StaticResourcesCache.getStaticResCache();
            if (staticResCache != null) {
              String path = StaticResourcesCache.getHttpConfig().getPath(changedFile);
              staticResCache.remove(path);
            }
          } catch (IOException e) {
            log.error(e.toString(), e);
          }
        }
      }

      // 重置 key，使其继续接收该目录下的事件；如果返回 false，表示监控目录不再有效（可能已被删除）
      boolean valid = key.reset();
      if (!valid) {
        keyPathMap.remove(key);
        if (keyPathMap.isEmpty()) {
          // 如果没有任何目录在监听，就退出
          break;
        }
      }
    }
  }
}
