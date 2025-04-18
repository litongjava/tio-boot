package com.litongjava.tio.boot.http.handler.common;

import java.io.File;
import java.io.RandomAccessFile;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.ResponseHeaderKey;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.FilenameUtils;

public class StaticResourcesHandler {

  public HttpResponse index(HttpRequest request) {
    String path = request.getRequestLine().getPath();
    HttpResponse response = TioRequestContext.getResponse();
    CORSUtils.enableCORS(response);
    File file = new File("." + File.separator + path);
    String suffix = FilenameUtils.getSuffix(path);
    String contentType = ContentTypeUtils.getContentType(suffix);

    if (!file.exists()) {
      response.setStatus(404);
      return response;
    }

    long fileLength = file.length();
    // 检查是否存在 Range 头信息
    String range = request.getHeader("Range");
    if (range != null && range.startsWith("bytes=")) {
      // 例如 Range: bytes=0-1023
      String rangeValue = range.substring("bytes=".length());
      String[] parts = rangeValue.split("-");
      try {
        long start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
        long end = (parts.length > 1 && !parts[1].isEmpty()) ? Long.parseLong(parts[1]) : fileLength - 1;
        // 检查 range 合法性
        if (start > end || end >= fileLength) {
          response.setStatus(416); // Range Not Satisfiable
          return response;
        }
        long contentLength = end - start + 1;
        byte[] data = new byte[(int) contentLength];

        // 使用 RandomAccessFile 来读取部分内容
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
          raf.seek(start);
          raf.readFully(data);
        }

        // 设置响应头
        response.setStatus(206); // Partial Content
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Accept-Ranges", "bytes");
        //RequestHeaderKey.Content_Length

        response.setHeader(ResponseHeaderKey.Content_Length, String.valueOf(contentLength));
        Resps.bytesWithContentType(response, data, contentType);
      } catch (Exception e) {
        response.setStatus(416);
      }
    } else {
      // 如果没有 Range 头，则直接返回整个文件
      byte[] readBytes = FileUtil.readBytes(file);
      response.setHeader("Accept-Ranges", "bytes");
      Resps.bytesWithContentType(response, readBytes, contentType);
    }
    //视频文件（如 mp4）本身已经是压缩格式，再进行 gzip 压缩可能会破坏文件格式，导致浏览器无法正确解码。
    response.setHasGzipped(true);
    return response;
  }
}
