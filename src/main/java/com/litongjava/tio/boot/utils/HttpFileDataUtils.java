package com.litongjava.tio.boot.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.ResponseHeaderKey;
import com.litongjava.tio.http.server.util.Resps;

public class HttpFileDataUtils {
  private static final Logger log = LoggerFactory.getLogger(HttpFileDataUtils.class);
  
  /** 1MB 阈值：大于等于该值走零拷贝 */
  public static final long ZERO_COPY_THRESHOLD = 1024 * 1024;

  /** GMT/RFC1123 时间格式 */
  public static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
      .withZone(ZoneId.of("GMT"));

  /**
   * 设置缓存相关响应头
   */
  public static void setCacheHeaders(HttpResponse response, long lastModified, String etag, String contentType,
      String suffix) {

    // m3u8 不缓存（并允许范围探测头，但后续会忽略 Range）
    if ("m3u8".equalsIgnoreCase(suffix)) {
      response.setHeader(ResponseHeaderKey.Cache_Control, "no-store, no-cache, must-revalidate, max-age=0");
      response.setHeader(ResponseHeaderKey.Pragma, "no-cache");
      response.setHeader(ResponseHeaderKey.Expires, "0");
      response.setHeader(ResponseHeaderKey.Accept_Ranges, "bytes");
      return;
    }

    // Last-Modified
    String lastModStr = HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(lastModified));
    response.setHeader(ResponseHeaderKey.Last_Modified, lastModStr);

    // ETag
    response.setHeader(ResponseHeaderKey.ETag, etag);

    // Cache-Control
    String cacheControl = getCacheControlForContentType(contentType);
    response.setHeader(ResponseHeaderKey.Cache_Control, cacheControl);

    // Expires：强缓存资源设为一年
    long expiresTime = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
    String expiresStr = HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(expiresTime));
    response.setHeader(ResponseHeaderKey.Expires, expiresStr);

    // Vary
    response.setHeader(ResponseHeaderKey.vary, "Accept-Encoding");
  }

  /**
   * 客户端缓存是否仍有效（If-None-Match / If-Modified-Since）
   */
  public static boolean isClientCacheValid(HttpRequest request, long lastModified, String etag) {
    // If-None-Match
    String ifNoneMatch = request.getHeader("if-none-match");
    if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
      return true;
    }

    // If-Modified-Since
    String ifModifiedSince = request.getHeader("if-modified-since");
    if (ifModifiedSince != null) {
      try {
        ZonedDateTime clientDate = ZonedDateTime.parse(ifModifiedSince, HTTP_DATE_FORMAT);
        Instant fileInstant = Instant.ofEpochMilli(lastModified);
        if (!fileInstant.isAfter(clientDate.toInstant())) {
          return true;
        }
      } catch (Exception ignore) {
        // 解析失败忽略
      }
    }

    return false;
  }

  /**
   * 按内容类型返回合理的 Cache-Control
   */
  public static String getCacheControlForContentType(String contentType) {
    if (contentType == null) {
      return "public, max-age=3600";
    }
    if (contentType.startsWith("image/")) {
      return "public, max-age=31536000, immutable";
    } else if (contentType.startsWith("video/") || contentType.startsWith("audio/")) {
      return "public, max-age=31536000, immutable";
    } else if (contentType.equals("text/css") || contentType.equals("application/javascript")
        || contentType.equals("text/javascript")) {
      return "public, max-age=31536000, immutable";
    } else if (contentType.startsWith("font/") || contentType.equals("application/font-woff")
        || contentType.equals("application/font-woff2")) {
      return "public, max-age=31536000, immutable";
    } else if (contentType.startsWith("text/")) {
      return "public, max-age=3600";
    } else {
      return "public, max-age=86400";
    }
  }

  /**
   * 生成简易 ETag（MD5 路径+长度+修改时间）
   */
  public static String generateETag(File file, long lastModified, long fileLength) {
    try {
      String input = file.getAbsolutePath() + fileLength + lastModified;
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(input.getBytes());

      StringBuilder sb = new StringBuilder();
      sb.append('"');
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      sb.append('"');
      return sb.toString();
    } catch (Exception e) {
      return "\"" + lastModified + "-" + fileLength + "\"";
    }
  }

  /**
   * 处理 Range 请求（单段）。对 m3u8 将忽略 Range，返回 200 全量。
   */
  public static HttpResponse handleRangeRequest(HttpResponse response, File file, String range, long fileLength,
      String contentType) {

    // 对 m3u8：一律返回 200 全量，避免播放器解析异常（0:00）
    if (isFullFileResponse(contentType)) {
      return handleFullFileRequest(response, file, fileLength, contentType);
    }

    if (range == null) {
      response.setStatus(400);
      return response;
    }

    String raw = range.trim();
    String lower = raw.toLowerCase(Locale.ROOT);
    if (!lower.startsWith("bytes=")) {
      // 不规范的 Range，宽容处理为 200 全量
      return handleFullFileRequest(response, file, fileLength, contentType);
    }

    // 不支持多重范围：发现逗号则退回 200 全量（或可改成 416）
    String rangeValue = lower.substring("bytes=".length());
    if (rangeValue.contains(",")) {
      log.info("Multi-range not supported, fallback to full response: {}", raw);
      return handleFullFileRequest(response, file, fileLength, contentType);
    }

    String[] parts = rangeValue.split("-", -1); // 保留空串
    try {
      if (parts.length != 2) {
        response.setStatus(416);
        response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
        return response;
      }

      String s = parts[0];
      String e = parts[1];

      long start, end;

      if (!s.isEmpty()) {
        // bytes=START- 或 bytes=START-END
        start = Long.parseLong(s);
        if (start < 0 || start >= fileLength) {
          response.setStatus(416);
          response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
          return response;
        }
        if (!e.isEmpty()) {
          end = Long.parseLong(e);
          if (end < start || end >= fileLength) {
            response.setStatus(416);
            response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
            return response;
          }
        } else {
          end = fileLength - 1;
        }
      } else {
        // 后缀范围：bytes=-N 取最后 N 个字节
        if (e.isEmpty()) {
          response.setStatus(416);
          response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
          return response;
        }
        long suffixLen = Long.parseLong(e);
        if (suffixLen <= 0) {
          response.setStatus(416);
          response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
          return response;
        }
        if (suffixLen >= fileLength) {
          start = 0;
        } else {
          start = fileLength - suffixLen;
        }
        end = fileLength - 1;
      }

      long contentLength = end - start + 1;

      // 决定零拷贝或小块内存返回
      if (contentLength >= ZERO_COPY_THRESHOLD) {
        return buildZeroCopyResponse(response, file, start, end, contentType, true, fileLength);
      } else {
        byte[] data = readFileRange(file, start, contentLength);
        if (data == null) {
          log.error("Failed to read file range: {}", file.getPath());
          response.setStatus(500);
          response.setBody("Internal Server Error".getBytes());
          return response;
        }

        response.setStatus(206);
        response.setHeader(ResponseHeaderKey.Content_Range, "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader(ResponseHeaderKey.Accept_Ranges, "bytes");
        response.setHeader(ResponseHeaderKey.Content_Length, String.valueOf(contentLength));
        response.setSkipAddContentLength(true);
        Resps.bytesWithContentType(response, data, contentType);

        // 媒体与 HLS 不压缩
        if (shouldSkipGzipFor(contentType)) {
          response.setSkipGzipped(true);
        } else {
          response.setSkipGzipped(false);
        }

        return response;
      }

    } catch (NumberFormatException ex) {
      log.error("Failed to parse range: {}", range, ex);
      response.setStatus(416);
      response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
      return response;
    } catch (Exception e2) {
      log.error("Error handling range request", e2);
      response.setStatus(500);
      response.setBody("Internal Server Error".getBytes());
      return response;
    }
  }

  /**
   * 处理整文件响应；m3u8 始终返回 200 全量，且禁用 gzip。
   */
  public static HttpResponse handleFullFileRequest(HttpResponse response, File file, long fileLength,
      String contentType) {
    try {
      if (fileLength >= ZERO_COPY_THRESHOLD) {
        // 大文件零拷贝
        buildZeroCopyResponse(response, file, 0, fileLength - 1, contentType, false, fileLength);
      } else {
        // 小文件读入内存
        byte[] fileData = readFullFile(file);
        if (fileData == null) {
          log.error("Failed to read file: {}", file.getPath());
          response.setStatus(500);
          response.setBody("Internal Server Error".getBytes());
          return response;
        }

        response.setStatus(200);
        response.setHeader(ResponseHeaderKey.Accept_Ranges, "bytes");
        Resps.bytesWithContentType(response, fileData, contentType);

        if (shouldSkipGzipFor(contentType)) {
          response.setSkipGzipped(true);
        } else {
          response.setSkipGzipped(false);
        }
      }
    } catch (Exception e) {
      log.error("Error handling full file request", e);
      response.setStatus(500);
      response.setBody("Internal Server Error".getBytes());
    }

    return response;
  }

  /**
   * 读取文件部分字节
   */
  public static byte[] readFileRange(File file, long start, long length) {
    if (length <= 0) {
      log.warn("Invalid length: {}", length);
      return new byte[0];
    }

    try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {

      long fileSize = channel.size();
      if (start >= fileSize) {
        log.warn("Start position {} exceeds file size {}", start, fileSize);
        return null;
      }

      long actualLength = Math.min(length, fileSize - start);
      ByteBuffer buffer = ByteBuffer.allocate((int) actualLength);
      channel.position(start);

      int totalBytesRead = 0;
      while (totalBytesRead < actualLength) {
        int read = channel.read(buffer);
        if (read == -1) {
          break;
        }
        totalBytesRead += read;
      }

      log.debug("Read {} bytes from position {} (requested {})", totalBytesRead, start, length);
      return buffer.array();
    } catch (IOException e) {
      log.error("Error reading file range: file={}, start={}, length={}", file.getPath(), start, length, e);
      return null;
    }
  }

  /**
   * 读取整文件
   */
  public static byte[] readFullFile(File file) {
    try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {

      long fileSize = channel.size();
      if (fileSize > Integer.MAX_VALUE) {
        log.error("File too large to read into memory: {}", fileSize);
        return null;
      }

      ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
      while (buffer.hasRemaining()) {
        if (channel.read(buffer) == -1) {
          break;
        }
      }

      log.debug("Read full file: {} bytes", fileSize);
      return buffer.array();
    } catch (IOException e) {
      log.error("Error reading full file: {}", file.getPath(), e);
      return null;
    }
  }

  /**
   * 构造零拷贝响应（支持 200 与 206）
   */
  public static HttpResponse buildZeroCopyResponse(HttpResponse response, File file, long start, long end,
      String contentType, boolean isRange, long fileLength) {

    long contentLength = end - start + 1;

    if (isRange) {
      response.setStatus(206);
      response.setHeader(ResponseHeaderKey.Content_Range, "bytes " + start + "-" + end + "/" + fileLength);
    } else {
      response.setStatus(200);
    }

    response.setHeader(ResponseHeaderKey.Accept_Ranges, "bytes");
    if (contentType != null) {
      response.setContentType(contentType);
    }
    response.setHeader(ResponseHeaderKey.Content_Length, String.valueOf(contentLength));
    response.setSkipAddContentLength(true);

    // 文件 body 交给下层传输
    response.setFileBody(file);
    response.setSkipGzipped(true); // 零拷贝路径禁用 gzip
    log.info("Zero copy: {} [{}-{}] len={}", file.getPath(), start, end, contentLength);
    return response;
  }

  /**
   * m3u8 类型判断
   */
  private static boolean isFullFileResponse(String contentType) {
    if (contentType == null) {
      return false;
    }
    String ct = contentType.toLowerCase(Locale.ROOT);
    return "application/vnd.apple.mpegurl".equals(ct) || "application/x-mpegurl".equals(ct) || "video/mp2t".equals(ct);
  }

  /**
   * 媒体/HLS/TS 等类型禁用 gzip
   */
  private static boolean shouldSkipGzipFor(String contentType) {
    if (contentType == null)
      return false;
    String ct = contentType.toLowerCase(Locale.ROOT);
    return ct.startsWith("video/") || ct.startsWith("audio/") || "application/vnd.apple.mpegurl".equals(ct)
        || "application/x-mpegurl".equals(ct) || "video/mp2t".equals(ct);
  }
}
