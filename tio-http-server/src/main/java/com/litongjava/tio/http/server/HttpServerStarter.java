package com.litongjava.tio.http.server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.TcpConst;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpId;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.http.common.TioConfigKey;
import com.litongjava.tio.http.common.handler.ITioHttpRequestHandler;
import com.litongjava.tio.http.common.session.id.impl.UUIDSessionIdGenerator;
import com.litongjava.tio.server.ServerTioConfig;
import com.litongjava.tio.server.TioServer;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheFactory;
import com.litongjava.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;
import com.litongjava.tio.utils.http.HttpUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.Json;

import okhttp3.Response;

/**
 *
 * @author tanyaowu
 */
public class HttpServerStarter {
  private static Logger log = LoggerFactory.getLogger(HttpServerStarter.class);
  private HttpConfig httpConfig = null;
  private HttpServerAioHandler httpServerAioHandler = null;
  private HttpServerAioListener httpServerAioListener = null;
  private ServerTioConfig serverTioConfig = null;
  private TioServer tioServer = null;
  private ITioHttpRequestHandler httpRequestHandler = null;
  /**
   * 预访问路径的后缀
   */
  private List<String> preAccessFileType = new ArrayList<>();

  /**
   * 
   * @param httpConfig
   * @param requestHandler
   * @author tanyaowu
   */
  public HttpServerStarter(HttpConfig httpConfig, ITioHttpRequestHandler requestHandler) {
    // preAccessFileType.add("css");
    // preAccessFileType.add("js");
    // preAccessFileType.add("jsp");
    preAccessFileType.add("html");
    preAccessFileType.add("ftl");
    // preAccessFileType.add("xml");
    // preAccessFileType.add("htm");

    init(httpConfig, requestHandler);
  }

  /**
   * @return the httpConfig
   */
  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public ITioHttpRequestHandler getHttpRequestHandler() {
    return httpRequestHandler;
  }

  /**
   * @return the httpServerAioHandler
   */
  public HttpServerAioHandler getHttpServerAioHandler() {
    return httpServerAioHandler;
  }

  /**
   * @return the httpServerAioListener
   */
  public HttpServerAioListener getHttpServerAioListener() {
    return httpServerAioListener;
  }

  /**
   * @return the serverTioConfig
   */
  public ServerTioConfig getServerTioConfig() {
    return serverTioConfig;
  }

  private void init(HttpConfig httpConfig, ITioHttpRequestHandler requestHandler) {
    String system_timer_period = System.getProperty("tio.system.timer.period");
    if (StrUtil.isBlank(system_timer_period)) {
      System.setProperty("tio.system.timer.period", "50");
    }

    this.httpConfig = httpConfig;
    this.httpRequestHandler = requestHandler;
    httpConfig.setHttpRequestHandler(this.httpRequestHandler);
    this.httpServerAioHandler = new HttpServerAioHandler(httpConfig, requestHandler);
    httpServerAioListener = new HttpServerAioListener();
    String name = httpConfig.getName();
    if (StrUtil.isBlank(name)) {
      name = "Tio Http Server";
    }
    serverTioConfig = new ServerTioConfig(name);
    serverTioConfig.setServerAioListener(httpServerAioListener);
    serverTioConfig.setServerAioHandler(httpServerAioHandler);
    serverTioConfig.setHeartbeatTimeout(1000 * 20);
    serverTioConfig.setShortConnection(true);
    serverTioConfig.setReadBufferSize(TcpConst.MAX_DATA_LENGTH);

    // serverTioConfig.setAttribute(TioConfigKey.HTTP_SERVER_CONFIG, httpConfig);
    serverTioConfig.setCacheFactory(ConcurrentMapCacheFactory.INSTANCE);
    serverTioConfig.setAttribute(TioConfigKey.HTTP_REQ_HANDLER, this.httpRequestHandler);
    serverTioConfig.setDefaultIpRemovalListenerWrapper();
    tioServer = new TioServer(serverTioConfig);

    HttpId imTioUuid = new HttpId();
    serverTioConfig.setTioUuid(imTioUuid);
  }

  public void setHttpRequestHandler(ITioHttpRequestHandler requestHandler) {
    this.httpRequestHandler = requestHandler;
  }

  public void start() throws IOException {
    start(false);
  }

  /**
   * @param preAccess
   * @throws IOException
   * @author tanyaowu
   */
  public void start(boolean preAccess) throws IOException {
    serverTioConfig.init();
    if (httpConfig.isUseSession()) {
      if (httpConfig.getSessionStore() == null) {
        CacheFactory cacheFactory = serverTioConfig.getCacheFactory();

        if (cacheFactory != null) {
          AbsCache absCache = cacheFactory.register(httpConfig.getSessionCacheName(), null, httpConfig.getSessionTimeout());
          httpConfig.setSessionStore(absCache);
        }
      }

      if (httpConfig.getSessionIdGenerator() == null) {
        httpConfig.setSessionIdGenerator(UUIDSessionIdGenerator.INSTANCE);
      }
    }

    tioServer.start(this.httpConfig.getBindIp(), this.httpConfig.getBindPort());

    if (preAccess) {
      preAccess();
    }
  }

  /**
   * 预访问第一版功能先上，后面再优化
   * 
   * @author tanyaowu
   */
  public void preAccess() {
    if (httpConfig.isPageInClasspath()) {
      log.info("暂时只支持目录形式的预访问");
      return;
    }

    String pageRoot = httpConfig.getPageRoot();
    if (pageRoot == null) {
      return;
    }

    new Thread(new Runnable() {
      @Override
      public void run() {
        Map<String, Long> pathCostMap = new TreeMap<>();

        long start = System.currentTimeMillis();
        preAccess(pageRoot, pathCostMap);
        long end = System.currentTimeMillis();
        long iv = end - start;

        Map<Long, Set<String>> costPathsMap = new TreeMap<>(new Comparator<Long>() {
          @Override
          public int compare(Long o1, Long o2) {
            // 倒序排序
            return Long.compare(o2, o1);
          }
        });
        Set<Entry<String, Long>> entrySet = pathCostMap.entrySet();
        for (Entry<String, Long> entry : entrySet) {
          try {
            Long cost = entry.getValue();
            String path = entry.getKey();
            Set<String> pathSet = costPathsMap.get(cost);
            if (pathSet == null) {
              pathSet = new TreeSet<>();
              costPathsMap.put(cost, pathSet);
            }
            boolean added = pathSet.add(path);
            if (!added) {
              log.error("可能重复访问了:{}", path);
            }
          } catch (Exception e) {
            log.error(e.toString(), e);
          }
        }

        Json json = Json.getJson();
        log.info("预访问了{}个path，耗时:{}ms，访问详情:\r\n{}\r\n耗时排序:\r\n{}", pathCostMap.size(), iv, json.toJson(pathCostMap), json.toJson(costPathsMap));
      }
    }).start();

  }

  /**
   * 预访问第一版功能先上，后面再优化
   * 
   * @author tanyaowu
   */
  private void preAccess(String rootpath, Map<String, Long> pathCostMap) {
    try {
      Map<String, String> headerMap = new HashMap<>();
      headerMap.put(RequestHeaderKey.Host, "127.0.0.1");

      String protocol = null;

      if (serverTioConfig.isSsl()) {
        protocol = "https";
      } else {
        protocol = "http";
      }
      String completePathPrefix = protocol + "://127.0.0.1:" + httpConfig.getBindPort();

      File rootDir = new File(rootpath);
      File[] files = rootDir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File file) {
          // String absolutePath = file.getAbsolutePath();
          String filename = file.getName();
          String extension = FileUtil.extName(filename);// .getExtension(filename);
          if (file.isDirectory()) {
            if ("svn-base".equalsIgnoreCase(extension)) {
              return false;
            }
            return true;
          }

          String ext = FileUtil.extName(file);
          if (preAccessFileType.contains(ext)) {
            return true;
          }
          return false;
        }
      });

      File pageRootFile = new File(httpConfig.getPageRoot());
      String pageRootAbs = pageRootFile.getCanonicalPath();
      for (File file : files) {
        try {
          if (file.isDirectory()) {
            preAccess(file.getCanonicalPath(), pathCostMap);
          } else {
            String absPath = file.getCanonicalPath();
            log.info("pageRoot:{}, 预访问路径getAbsolutePath:{}", httpConfig.getPageRoot(), absPath);
            long start = System.currentTimeMillis();
            String path = absPath.substring(pageRootAbs.length());

            if (!(path.startsWith("/") || path.startsWith("\\"))) {
              path = "/" + path;
            }
            log.info("预访问路径:{}", path);
            String url = completePathPrefix + path;
            Response response = HttpUtils.get(url, headerMap);
            long end = System.currentTimeMillis();
            long iv = end - start;
            pathCostMap.put(path, iv);
            log.info("预访问完成，耗时{}ms, [{}], {}", iv, path, response);
            response.close();
          }
        } catch (Exception e) {
          log.error(e.toString());
        }
      }
    } catch (Exception e) {
      log.error("预访问报错", e);
    }
  }

  public void stop() throws IOException {
    tioServer.stop();
  }

  public TioServer getTioServer() {
    return tioServer;
  }

}
