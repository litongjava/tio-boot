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

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.ResponseHeaderKey;
import com.litongjava.tio.http.server.util.Resps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpFileDataUtils {

  public static final long ZERO_COPY_THRESHOLD = 1024 * 1024; // 1MB
  public static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
      .withZone(ZoneId.of("GMT"));

  public static void setCacheHeaders(HttpResponse response, long lastModified, String etag, String contentType,
      String suffix) {

    // m3u8 不缓存
    if ("m3u8".equalsIgnoreCase(suffix)) {
      response.setHeader(ResponseHeaderKey.Cache_Control, "no-store, no-cache, must-revalidate, max-age=0");
      response.setHeader(ResponseHeaderKey.Pragma, "no-cache");
      response.setHeader(ResponseHeaderKey.Expires, "0");
      response.setHeader(ResponseHeaderKey.Accept_Ranges, "bytes");
      return;
    }

    // 设置 Last-Modified
    String lastModStr = HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(lastModified));
    response.setHeader(ResponseHeaderKey.Last_Modified, lastModStr);

    // 设置 ETag
    response.setHeader(ResponseHeaderKey.ETag, etag);

    // 设置 Cache-Control - 根据文件类型设置不同的缓存策略
    String cacheControl = getCacheControlForContentType(contentType);
    response.setHeader(ResponseHeaderKey.Cache_Control, cacheControl);

    // 设置 Expires (1年后过期,适用于静态资源)
    long expiresTime = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
    String expiresStr = HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(expiresTime));
    response.setHeader(ResponseHeaderKey.Expires, expiresStr);

    // 设置 Vary 头部
    response.setHeader(ResponseHeaderKey.vary, "Accept-Encoding");
  }

  public static boolean isClientCacheValid(HttpRequest request, long lastModified, String etag) {
    // 检查 If-None-Match (ETag)
    String ifNoneMatch = request.getHeader("if-none-match");
    if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
      return true;
    }

    // 检查 If-Modified-Since
    String ifModifiedSince = request.getHeader("if-modified-since");
    if (ifModifiedSince != null) {
      try {
        ZonedDateTime clientDate = ZonedDateTime.parse(ifModifiedSince, HTTP_DATE_FORMAT);
        Instant fileInstant = Instant.ofEpochMilli(lastModified);
        if (!fileInstant.isAfter(clientDate.toInstant())) {
          return true;
        }
      } catch (Exception e) {
        // 解析失败,忽略
      }
    }

    return false;
  }

  public static String getCacheControlForContentType(String contentType) {
    if (contentType == null) {
      return "public, max-age=3600";
    }
    if (contentType.startsWith("image/")) {
      return "public, max-age=31536000, immutable";
    } else if (contentType.startsWith("video/") || contentType.startsWith("audio/")) {
      return "public, max-age=31536000, immutable";
    } else if (contentType.equals("text/css") || contentType.equals("application/javascript")) {
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

  public static HttpResponse handleRangeRequest(HttpResponse response, File file, String range, long fileLength,
      String contentType) {
    String rangeValue = range.substring("bytes=".length());
    String[] parts = rangeValue.split("-", -1); // 使用 -1 确保空字符串也被保留

    try {
      long start = 0;
      long end = fileLength - 1;

      // 解析 start
      if (parts.length > 0 && !parts[0].isEmpty()) {
        start = Long.parseLong(parts[0]);
      }

      // 解析 end
      if (parts.length > 1 && !parts[1].isEmpty()) {
        end = Long.parseLong(parts[1]);
      }

      // 验证范围
      if (start < 0 || start >= fileLength || end < start || end >= fileLength) {
        log.warn("Invalid range: start={}, end={}, fileLength={}", start, end, fileLength);
        response.setStatus(416);
        response.setHeader(ResponseHeaderKey.Content_Range, "bytes */" + fileLength);
        return response;
      }

      long contentLength = end - start + 1;
      log.warn("Range request: bytes={}-{}/{}, contentLength={}", start, end, fileLength, contentLength);

      if (contentLength >= ZERO_COPY_THRESHOLD) {
        // 大 range 也走零拷贝
        buildZeroCopyResponse(response, file, start, end, contentType, true, fileLength);
      } else {
        // 小范围读进内存
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

        // M3U8 和 TS 文件都不应该被 gzip 压缩
        if (contentType != null && (contentType.startsWith("video/") || contentType.startsWith("audio/")
            || contentType.equals("application/vnd.apple.mpegurl") || contentType.equals("application/x-mpegURL"))) {
          response.setSkipGzipped(true);
        } else {
          response.setSkipGzipped(false);
        }
      }
    } catch (NumberFormatException e) {
      log.error("Failed to parse range: {}", range, e);
      response.setStatus(416);
      response.setHeader("Content-Range", "bytes */" + fileLength);
    } catch (Exception e) {
      log.error("Error handling range request", e);
      response.setStatus(500);
      response.setBody("Internal Server Error".getBytes());
    }

    return response;
  }

  public static HttpResponse handleFullFileRequest(HttpResponse response, File file, long fileLength,
      String contentType) {
    try {
      if (fileLength >= ZERO_COPY_THRESHOLD) {
        // 大文件走零拷贝
        buildZeroCopyResponse(response, file, 0, fileLength - 1, contentType, false, fileLength);
      } else {
        // 小文件读进内存
        byte[] fileData = readFullFile(file);
        if (fileData == null) {
          log.error("Failed to read file: {}", file.getPath());
          response.setStatus(500);
          response.setBody("Internal Server Error".getBytes());
          return response;
        }

        response.setHeader(ResponseHeaderKey.Accept_Ranges, "bytes");
        Resps.bytesWithContentType(response, fileData, contentType);

        // M3U8 和媒体文件不压缩
        if (contentType != null && (contentType.startsWith("video/") || contentType.startsWith("audio/")
            || contentType.equals("application/vnd.apple.mpegurl") || contentType.equals("application/x-mpegURL"))) {
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

  public static byte[] readFileRange(File file, long start, long length) {
    if (length <= 0) {
      log.warn("Invalid length: {}", length);
      return new byte[0];
    }

    try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {

      // 检查文件大小
      long fileSize = channel.size();
      if (start >= fileSize) {
        log.warn("Start position {} exceeds file size {}", start, fileSize);
        return null;
      }

      // 调整长度以防止超出文件末尾
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

  public static HttpResponse buildZeroCopyResponse(HttpResponse response, File file, long start, long end,
      String contentType, boolean isRange, long fileLength) {
    String path = file.getPath();
    log.info("Zero copy from {} start {} end {}", path, start, end);
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

    // 把文件 body 交给下层 transfer 逻辑去处理
    response.setFileBody(file);
    response.setSkipGzipped(true);
    return response;
  }
}