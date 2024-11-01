package com.litongjava.tio.boot.http.handler.internal;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.cache.AbsCache;

/**
 * @author litongjava
 */
public class FileChangeListener implements FileAlterationListener {
  private static Logger log = LoggerFactory.getLogger(FileChangeListener.class);
  private TioBootHttpRequestDispatcher defaultHttpRequestHandler = null;

  FileChangeListener(TioBootHttpRequestDispatcher defaultHttpRequestHandler) {
    this.defaultHttpRequestHandler = defaultHttpRequestHandler;
  }

  public void removeCache(File file) {
    try {
      AbsCache staticResCache = defaultHttpRequestHandler.getStaticResCache();
      if (staticResCache != null) {
        String path = defaultHttpRequestHandler.httpConfig.getPath(file);
        staticResCache.remove(path);
      }
    } catch (IOException e) {
      log.error(e.toString(), e);
    }
  }

  @Override
  public void onStart(FileAlterationObserver fileAlterationObserver) {
    // System.out.println("monitor start scan files..");
  }

  @Override
  public void onDirectoryCreate(File file) {
    // System.out.println(file.getName() + " director created.");
  }

  @Override
  public void onDirectoryChange(File file) {

  }

  @Override
  public void onDirectoryDelete(File file) {
    // System.out.println(file.getName() + " director deleted.");

  }

  @Override
  public void onFileCreate(File file) {
    // String name = file.getName();
    // String substring = name.substring(0, 8);
    // System.out.println("时间为：" + substring);
    // System.out.println(name + " created.");
  }

  @Override
  public void onFileChange(File file) {
    // System.out.println(file.getName() + " changed.");
    removeCache(file);
  }

  @Override
  public void onFileDelete(File file) {
    // System.out.println(file.getName() + " deleted.");
    removeCache(file);
  }

  @Override
  public void onStop(FileAlterationObserver fileAlterationObserver) {
    // System.out.println("monitor stop scanning..");
  }
}
