package com.litongjava.tio.utils.date;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.litongjava.tio.utils.map.SyncWriteMap;

public class TioTimeUtils {
  
  /**
   * 缓存线程安全的 DateTimeFormatter
   */
  private static final Map<String, DateTimeFormatter> formaters = new SyncWriteMap<>();

  /**
   * 结合 ThreadLocal 缓存 "非线程安全" 的 SimpleDateFormat
   */
  private static final ThreadLocal<HashMap<String, SimpleDateFormat>> TL = ThreadLocal
      .withInitial(() -> new HashMap<>());

  public static SimpleDateFormat getSimpleDateFormat(String pattern) {
    SimpleDateFormat ret = TL.get().get(pattern);
    if (ret == null) {
      ret = new SimpleDateFormat(pattern);
      TL.get().put(pattern, ret);
    }
    return ret;
  }

  public static String format(Date date) {
    return format(date, "yyyy-MM-dd HH:mm:ss");
  }

  /**
   * 按指定 pattern 将 Date 转换成 String
   * 例如：format(new Date(), "yyyy-MM-dd HH:mm:ss")
   */
  public static String format(Date date, String pattern) {
    return getSimpleDateFormat(pattern).format(date);
  }
  
  /**
   * 按指定 pattern 将 LocalDate 转换成 String
   */
  public static String format(LocalDate localDate, String pattern) {
    return localDate.format(getDateTimeFormatter(pattern));
  }
  
  public static DateTimeFormatter getDateTimeFormatter(String pattern) {
    DateTimeFormatter ret = formaters.get(pattern);
    if (ret == null) {
      ret = DateTimeFormatter.ofPattern(pattern);
      formaters.put(pattern, ret);
    }
    return ret;
  }
  
  /**
   * 按指定 pattern 将 LocalDateTime 转换成 String
   * 例如：format(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")
   */
  public static String format(LocalDateTime localDateTime, String pattern) {
    return localDateTime.format(getDateTimeFormatter(pattern));
  }
  
  
  /**
   * 按指定 pattern 将 LocalTime 转换成 String
   */
  public static String format(LocalTime localTime, String pattern) {
    return localTime.format(getDateTimeFormatter(pattern));
  }
  
  /**
   * java.time.LocalDateTime --> java.util.Date
   */
  public static Date toDate(LocalDateTime localDateTime) {
    ZoneId zone = ZoneId.systemDefault();
    Instant instant = localDateTime.atZone(zone).toInstant();
    return Date.from(instant);
  }
}
