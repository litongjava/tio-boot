package com.litongjava.tio.boot.http.handler.common;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.utils.HttpFileDataUtils;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.FilenameUtils;

public class HttpFileDataHandler implements HttpRequestHandler {

  private static final Logger log = LoggerFactory.getLogger(HttpFileDataHandler.class);
  
  private boolean printUrl = true;

  public HttpFileDataHandler() {

  }

  public HttpFileDataHandler(boolean printUrl) {
    this.printUrl = printUrl;
  }

  @Override
  public HttpResponse handle(HttpRequest request) throws Exception {
    String path = request.getRequestLine().getPath();
    if (printUrl) {
      log.info(path);
    }

    HttpResponse response = TioRequestContext.getResponse();
    CORSUtils.enableCORS(response);

    File file = new File("." + File.separator + path);

    if (!file.exists()) {
      response.setStatus(404);
      return response;
    }

    // 生成 ETag
    long fileLength = file.length();
    long lastModified = file.lastModified();

    String etag = HttpFileDataUtils.generateETag(file, lastModified, fileLength);

    // 设置缓存相关头部
    String suffix = FilenameUtils.getSuffix(path);
    String contentType = ContentTypeUtils.getContentType(suffix);
    HttpFileDataUtils.setCacheHeaders(response, lastModified, etag, contentType, suffix);

    // 检查客户端缓存
    if (HttpFileDataUtils.isClientCacheValid(request, lastModified, etag)) {
      response.setStatus(304); // Not Modified
      return response;
    }

    // 检查是否存在 Range 头信息
    String range = request.getHeader("range");
    if (range != null && range.startsWith("bytes=")) {
      return HttpFileDataUtils.handleRangeRequest(response, file, range, fileLength, contentType);
    } else {
      return HttpFileDataUtils.handleFullFileRequest(response, file, fileLength, contentType);
    }
  }

}