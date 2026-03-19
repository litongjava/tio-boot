package com.litongjava.tio.utils.path;

import java.io.File;

public class WorkDirUtils {

  public static final String workingDir = ".";
  public static final String workingDataDir = "data";
  public static final String workingCacheDir = "cache";
  public static final String workingResourcesDir = "resources";
  public static final String workingPagesDir = "pages";
  public static final String workingScriptsDir = "scripts";
  public static final String workingMediaDir = "media";

  public static String getWorkingDir(String subPath) {
    File file = new File(subPath);
    if (!file.exists()) {
      file.mkdirs();
    }
    return file.getAbsolutePath();
  }

  public static String getWorkingDir() {
    File file = new File(workingDir);
    if (!file.exists()) {
      file.mkdirs();
    }
    return workingDir;
  }

  public static String workingDataDir() {
    File file = new File(workingDataDir);
    if (!file.exists()) {
      file.mkdirs();
    }
    return workingDataDir;
  }

  public static String workingResourcesDir() {
    File file = new File(workingResourcesDir);
    if (!file.exists()) {
      file.mkdirs();
    }
    return workingResourcesDir;
  }

  public static String workingPagesDir() {
    File file = new File(workingPagesDir);
    if (!file.exists()) {
      file.mkdirs();
    }
    return workingPagesDir;
  }

  public static String workingScriptsDir() {
    File file = new File(workingScriptsDir);
    if (!file.exists()) {
      file.mkdirs();
    }
    return workingScriptsDir;
  }

  public static String workingCacheDir() {
    File file = new File("cache");
    if (!file.exists()) {
      file.mkdirs();
    }
    return workingCacheDir;
  }
}
