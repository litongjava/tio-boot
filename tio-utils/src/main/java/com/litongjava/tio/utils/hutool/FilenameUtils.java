package com.litongjava.tio.utils.hutool;

import java.io.File;

public class FilenameUtils {

  public static String getSuffix(String filename) {
    if (filename == null || filename.isEmpty()) {
      return "";
    }
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex == -1 || dotIndex == filename.length() - 1) {
      return "";
    }
    return filename.substring(dotIndex + 1);
  }

  /**
   * 获取文件名，不包含路径
   * 
   * @param filename
   * @return
   */
  public static String getBaseName(String filename) {
    if (filename == null || filename.isEmpty()) {
      return "";
    }
    filename = new File(filename).getName();
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex == -1) {
      return filename;
    }
    return filename.substring(0, dotIndex);
  }

  /**
   * 获取路径中的子路径，比如传入kb/document/md/1.md，返回kb/document/md
   * 
   * @param path
   * @return 子路径
   */
  public static String getSubPath(String path) {
    if (path == null || path.isEmpty()) {
      return "";
    }
    int lastSlashIndex = path.lastIndexOf('/');
    if (lastSlashIndex == -1) {
      return "";
    }
    // 获取最后一个斜杠之前的路径
    String subPath = path.substring(0, lastSlashIndex);
    return subPath;
  }

  /**
   * 获取路径中的文件夹名称，比如传入kb/document/123456/1.md，返回123456
   * 
   * @param path
   * @return
   */
  public static String getParentFolderName(String path) {
    if (path == null || path.isEmpty()) {
      return "";
    }
    int lastSlashIndex = path.lastIndexOf('/');
    if (lastSlashIndex == -1) {
      return "";
    }
    String subPath = path.substring(0, lastSlashIndex);
    int parentSlashIndex = subPath.lastIndexOf('/');
    if (parentSlashIndex == -1) {
      return subPath;
    }
    return subPath.substring(parentSlashIndex + 1);
  }

  public static String getFilename(String path) {
    if (path == null || path.isEmpty()) {
      return "";
    }
    int lastSlashIndex = path.lastIndexOf('/');
    if (lastSlashIndex == -1) {
      lastSlashIndex = path.lastIndexOf('\\');
      if (lastSlashIndex == -1) {
        return path;
      }
    }
    String subPath = path.substring(lastSlashIndex + 1);
    return subPath;
  }

  public static boolean isImageFile(String name) {
    String lower = name.toLowerCase();
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")
        || lower.endsWith(".bmp");
  }

}
