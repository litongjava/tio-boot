package com.litongjava.tio.utils.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import com.litongjava.tio.utils.date.TioTimeUtils;

/**
 * JsonResult 用于存放 json 生成结果，结合 ThreadLocal 进行资源重用
 */
public class JsonResult {

  /**
   * 缓存 SimpleDateFormat
   * 
   * 备忘：请勿使用 TimeKit.getSimpleDateFormat(String) 优化这里，可减少一次
   *      ThreadLocal.get() 调用
   */
  Map<String, SimpleDateFormat> formats = new HashMap<>();

  // StringBuilder 内部对 int、long、double、float 数据写入有优化
  StringBuilder sb = new StringBuilder();

  String datePattern;
  String timestampPattern;
  boolean longToString;
  boolean inUse = false;

  public void init(String datePattern, String timestampPattern) {
    this.datePattern = datePattern;
    this.timestampPattern = timestampPattern;
    inUse = true;
  }

  public void init(String dp, String timestampPattern, boolean longToString) {
    this.datePattern = dp;
    this.timestampPattern = timestampPattern;
    this.longToString = longToString;
    inUse = true;
  }

  // 用来判断当前是否处于重入型转换状态，如果为 true，则要使用 new JsonResult()
  public boolean isInUse() {
    return inUse;
  }

  public void clear() {
    inUse = false;

    // 释放空间占用过大的缓存
    if (sb.length() > TioJsonKit.maxBufferSize) {
      sb = new StringBuilder(Math.max(1024, TioJsonKit.maxBufferSize / 2));
    } else {
      sb.setLength(0);
    }
  }

  public String toString() {
    return sb.toString();
  }

  public byte[] toBytes() {
    IntStream intStream = sb.chars();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      intStream.forEach(c -> {
        baos.write(c);
      });
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public int length() {
    return sb.length();
  }

  public void addChar(char ch) {
    sb.append(ch);
  }

  public void addNull() {
    // sb.append((String)null);
    sb.append("null");
  }

  public void addStr(String str) {
    TioJsonKit.escape(str, sb);
  }

  public void addStrNoEscape(String str) {
    sb.append('\"').append(str).append('\"');
  }

  public void addInt(int i) {
    sb.append(i);
  }

  public void addLong(long l) {
    if (longToString) {
      sb.append("\"" + l + "\"");
    } else {
      sb.append(l);
    }

  }

  public void addDouble(double d) {
    sb.append(d);
  }

  public void addFloat(float f) {
    sb.append(f);
  }

  public void addNumber(Number n) {
    sb.append(n.toString());
  }

  public void addBoolean(boolean b) {
    sb.append(b);
  }

  public void addEnum(@SuppressWarnings("rawtypes") Enum en) {
    sb.append('\"').append(en.toString()).append('\"');
  }

  public String getDatePattern() {
    return datePattern;
  }

  public String getTimestampPattern() {
    return timestampPattern;
  }

  public SimpleDateFormat getFormat(String pattern) {
    SimpleDateFormat ret = formats.get(pattern);
    if (ret == null) {
      ret = new SimpleDateFormat(pattern);
      formats.put(pattern, ret);
    }
    return ret;
  }

  public void addTime(Time t) {
    sb.append('\"').append(t.toString()).append('\"');
  }

  public void addTimestamp(Timestamp ts) {
    if (timestampPattern != null) {
      sb.append('\"').append(getFormat(timestampPattern).format(ts)).append('\"');
    } else {
      sb.append(ts.getTime());
    }
  }

  public void addDate(Date d) {
    if (datePattern != null) {
      sb.append('\"').append(getFormat(datePattern).format(d)).append('\"');
    } else {
      sb.append(d.getTime());
    }
  }

  public void addLocalDateTime(LocalDateTime ldt) {
    if (datePattern != null) {
      sb.append('\"').append(TioTimeUtils.format(ldt, datePattern)).append('\"');
    } else {
      sb.append(TioTimeUtils.toDate(ldt).getTime());
    }
  }

  public void addLocalDate(LocalDate ld) {
    // LocalDate 的 pattern 不支持时分秒
    // 可通过 JFinalJson.addToJson(LocalDate.class, ...) 定制自己的转换 pattern
    String dp = "yyyy-MM-dd";
    sb.append('\"').append(TioTimeUtils.format(ld, dp)).append('\"');
  }

  public void addLocalTime(LocalTime lt) {
    // LocalTime 的 pattern 不支持年月日，并且 LocalTime.toString() 的结果与 Time.toString() 格式不同
    // 可通过 JFinalJson.addToJson(LocalTime.class, ...) 定制自己的转换 pattern
    String tp = "HH:mm:ss";
    sb.append('\"').append(TioTimeUtils.format(lt, tp)).append('\"');
  }

  public void addMapKey(Object value) {
    TioJsonKit.escape(String.valueOf(value), sb);
  }

  public void addUnknown(Object obj) {
    TioJsonKit.escape(obj.toString(), sb);
  }

}