package com.litongjava.tio.utils.video;

public class VideoTimeUtils {
  /**
   * 将毫秒转换为 “时:分:秒” 格式。
   */
  public static String formatTime(long millis) {
    long totalSeconds = millis / 1000;
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }
}
