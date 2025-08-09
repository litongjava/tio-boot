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

public class HttpFileDataUtils {

  private static final long ZERO_COPY_THRESHOLD = 1024 * 1024; // 1MB
  private static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
      .withZone(ZoneId.of("GMT"));

  public static void setCacheHeaders(HttpResponse response, long lastModified, String etag, String contentType,
      String suffix) {

    // —— special-case: m3u8 不缓存 ——
    if ("m3u8".equalsIgnoreCase(suffix)) {
      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Expires", "0");
      return;
    }

    // 设置 Last-Modified
    String lastModStr = HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(lastModified));
    response.setHeader("Last-Modified", lastModStr);

    // 设置 ETag
    response.setHeader("ETag", etag);

    // 设置 Cache-Control - 根据文件类型设置不同的缓存策略
    String cacheControl = getCacheControlForContentType(contentType);
    response.setHeader("Cache-Control", cacheControl);

    // 设置 Expires (1年后过期，适用于静态资源)
    long expiresTime = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
    String expiresStr = HTTP_DATE_FORMAT.format(Instant.ofEpochMilli(expiresTime));
    response.setHeader("Expires", expiresStr);

    // 设置 Vary 头部
    response.setHeader("Vary", "Accept-Encoding");
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
        // 解析失败，忽略
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
    String[] parts = rangeValue.split("-");

    try {
      long start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
      long end = (parts.length > 1 && !parts[1].isEmpty()) ? Long.parseLong(parts[1]) : fileLength - 1;

      if (start > end || end >= fileLength) {
        response.setStatus(416);
        response.setHeader("Content-Range", "bytes */" + fileLength);
        return response;
      }

      long contentLength = end - start + 1;

      if (contentLength >= ZERO_COPY_THRESHOLD) {
        // 大 range 也走零拷贝
        prepareZeroCopyResponse(response, file, start, end, contentType, true, fileLength);
      } else {
        // 小范围还是读进内存
        byte[] data = readFileRange(file, start, contentLength);
        if (data == null) {
          response.setStatus(500);
          return response;
        }

        response.setStatus(206);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader(ResponseHeaderKey.Content_Length, String.valueOf(contentLength));
        Resps.bytesWithContentType(response, data, contentType);
        response.setSkipGzipped(false);
      }
    } catch (Exception e) {
      response.setStatus(416);
      response.setHeader("Content-Range", "bytes */" + fileLength);
    }

    return response;
  }

  public static HttpResponse handleFullFileRequest(HttpResponse response, File file, long fileLength,
      String contentType) {
    if (fileLength >= ZERO_COPY_THRESHOLD) {
      // 大文件走零拷贝（真正传输在下层 transfer 中做）
      prepareZeroCopyResponse(response, file, 0, fileLength - 1, contentType, false, fileLength);
    } else {
      // 小文件读进内存
      byte[] fileData = readFullFile(file);
      if (fileData == null) {
        response.setStatus(500);
        return response;
      }

      response.setHeader("Accept-Ranges", "bytes");
      Resps.bytesWithContentType(response, fileData, contentType);

      if (contentType != null && (contentType.startsWith("video/") || contentType.startsWith("audio/"))) {
        response.setSkipGzipped(true);
      } else {
        response.setSkipGzipped(false);
      }
    }

    return response;
  }

  public static byte[] readFileRange(File file, long start, long length) {
    try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {

      ByteBuffer buffer = ByteBuffer.allocate((int) length);
      channel.position(start);

      int bytesRead = 0;
      while (bytesRead < length) {
        int read = channel.read(buffer);
        if (read == -1) {
          break;
        }
        bytesRead += read;
      }
      return buffer.array();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] readFullFile(File file) {
    try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {

      long fileSize = channel.size();
      ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
      while (buffer.hasRemaining()) {
        if (channel.read(buffer) == -1)
          break;
      }
      return buffer.array();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static HttpResponse prepareZeroCopyResponse(HttpResponse response, File file, long start, long end,
      String contentType, boolean isRange, long fileLength) {
    long contentLength = end - start + 1;

    if (isRange) {
      response.setStatus(206);
      response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
    } else {
      response.setStatus(200);
    }

    response.setHeader("Accept-Ranges", "bytes");
    if (contentType != null) {
      response.setHeader("Content-Type", contentType);
    }
    response.setHeader(ResponseHeaderKey.Content_Length, String.valueOf(contentLength));
    response.setAddContentLength(false);

    // 把文件 body 交给下层 transfer 逻辑去处理（零拷贝/分块等在 SendPacketTask.transfer 里）
    response.setFileBody(file);
    // 这个字段表示 body 不需要再 gzip
    response.setSkipGzipped(false);
    return response;
  }
}
