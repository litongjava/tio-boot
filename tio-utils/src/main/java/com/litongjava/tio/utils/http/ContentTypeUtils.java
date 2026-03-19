package com.litongjava.tio.utils.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ContentTypeUtils {

  private static final Map<String, String> MIME_TYPES;

  /**
   * 根据文件扩展名返回 Content-Type。 传入可包含前导点或大小写（例如 ".JPG"），内部会标准化。 未知类型返回
   * "application/octet-stream"。
   */
  public static String getContentType(String fileExt) {
    if (fileExt == null || fileExt.isEmpty()) {
      return "application/octet-stream";
    }
    String key = normalizeExt(fileExt);
    String result = MIME_TYPES.get(key);
    return result != null ? result : "application/octet-stream";
  }

  private static String normalizeExt(String ext) {
    String e = ext.trim();
    if (e.startsWith(".")) {
      e = e.substring(1);
    }
    return e.toLowerCase();
  }

  static {
    Map<String, String> m = new HashMap<>();

    // --- 文本 / 标记 ---
    m.put("txt", "text/plain");
    m.put("csv", "text/csv");
    m.put("tsv", "text/tab-separated-values");
    m.put("css", "text/css");
    m.put("htm", "text/html");
    m.put("html", "text/html");
    m.put("xhtml", "application/xhtml+xml");
    m.put("xml", "application/xml");
    m.put("json", "application/json");
    m.put("map", "application/json"); // source map
    m.put("vtt", "text/vtt");
    m.put("yaml", "text/yaml");
    m.put("yml", "text/yaml");

    // --- 脚本 / 代码 ---
    m.put("js", "text/javascript"); // WHATWG 推荐；若需也可换成 application/javascript
    m.put("mjs", "text/javascript");
    m.put("ts", "application/typescript");
    m.put("jsx", "text/javascript");
    m.put("tsx", "application/typescript");
    m.put("java", "text/x-java-source");
    m.put("class", "application/java-vm");
    m.put("wasm", "application/wasm");

    // --- 图片 ---
    m.put("png", "image/png");
    m.put("jpg", "image/jpeg");
    m.put("jpeg", "image/jpeg");
    m.put("jfif", "image/jpeg");
    m.put("gif", "image/gif");
    m.put("bmp", "image/bmp");
    m.put("webp", "image/webp");
    m.put("ico", "image/x-icon"); // 或 image/vnd.microsoft.icon
    m.put("svg", "image/svg+xml");
    m.put("tif", "image/tiff");
    m.put("tiff", "image/tiff");
    m.put("avif", "image/avif");
    m.put("heic", "image/heic");
    m.put("heif", "image/heif");

    // --- 音频 ---
    m.put("mp3", "audio/mpeg");
    m.put("mpga", "audio/mpeg");
    m.put("wav", "audio/wav"); // 或 audio/x-wav
    m.put("ogg", "audio/ogg"); // Ogg 音频容器
    m.put("oga", "audio/ogg"); // Ogg+Vorbis/Opus 常用扩展
    m.put("m4a", "audio/mp4");
    m.put("aac", "audio/aac");
    m.put("flac", "audio/flac");
    m.put("mid", "audio/midi");
    m.put("midi", "audio/midi");
    m.put("weba", "audio/webm");
    m.put("m3u", "audio/x-mpegurl");
    m.put("m3u8", "application/vnd.apple.mpegurl");

    // --- 视频 ---
    m.put("mp4", "video/mp4");
    m.put("m4v", "video/mp4");
    m.put("mpeg", "video/mpeg");
    m.put("mpg", "video/mpeg");
    m.put("mp2", "video/mpeg"); // 旧扩展，若只存音频可去掉
    m.put("mpv", "video/mpeg");
    m.put("mov", "video/quicktime");
    m.put("avi", "video/x-msvideo");
    m.put("wmv", "video/x-ms-wmv");
    m.put("ogv", "video/ogg");
    m.put("webm", "video/webm");
    m.put("ts", "video/mp2t"); // HLS 分片
    m.put("3gp", "video/3gpp");
    m.put("3g2", "video/3gpp2");

    // --- 字体 ---
    m.put("woff", "font/woff");
    m.put("woff2", "font/woff2");
    m.put("ttf", "font/ttf");
    m.put("otf", "font/otf");
    m.put("eot", "application/vnd.ms-fontobject");

    // --- 文档 / 办公 ---
    m.put("pdf", "application/pdf");
    m.put("rtf", "application/rtf");
    m.put("doc", "application/msword");
    m.put("dot", "application/msword");
    m.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    m.put("ppt", "application/vnd.ms-powerpoint");
    m.put("pps", "application/vnd.ms-powerpoint");
    m.put("pot", "application/vnd.ms-powerpoint");
    m.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    m.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    m.put("xls", "application/vnd.ms-excel");
    m.put("xlw", "application/vnd.ms-excel");
    m.put("csvx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    // --- 压缩 / 包 ---
    m.put("zip", "application/zip");
    m.put("gz", "application/gzip");
    m.put("tar", "application/x-tar");
    m.put("tgz", "application/gzip"); // 或 application/x-compressed-tar
    m.put("bz2", "application/x-bzip2");
    m.put("7z", "application/x-7z-compressed");
    m.put("rar", "application/vnd.rar");

    // --- 其他常见 ---
    m.put("apk", "application/vnd.android.package-archive");
    m.put("ipa", "application/octet-stream"); // iOS 包未注册，统一 octet-stream
    m.put("xap", "application/x-silverlight-app");
    m.put("ics", "text/calendar");
    m.put("eml", "message/rfc822");

    MIME_TYPES = Collections.unmodifiableMap(m);
  }
}