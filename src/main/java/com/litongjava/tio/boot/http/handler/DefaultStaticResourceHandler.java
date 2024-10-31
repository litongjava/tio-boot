package com.litongjava.tio.boot.http.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

import com.litongjava.constatns.ServerConfigKeys;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResource;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.view.freemarker.FreemarkerConfig;
import com.litongjava.tio.http.server.handler.FileCache;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.IoUtils;
import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.freemarker.FreemarkerUtils;
import com.litongjava.tio.utils.hutool.ArrayUtil;
import com.litongjava.tio.utils.hutool.FileUtil;

import freemarker.template.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultStaticResourceHandler implements StaticResourceHandler {
  private static final long MAX_CACHE_FILE_SIZE = 5 * 1024 * 1024; // 最大缓存大小5MB

  public HttpResponse handle(String path, HttpRequest request, HttpConfig httpConfig, AbsCache staticResCache) {
    boolean enable = EnvUtils.getBoolean(ServerConfigKeys.SERVER_RESOURCES_STATIC_FILE_CACHE_ENABLE, false);
    HttpResponse response = null;
    FileCache fileCache = null;

    // 从缓存中获取FileCache
    if (enable && staticResCache != null) {
      fileCache = (FileCache) staticResCache.get(path);
    }

    if (enable && fileCache != null) {
      long lastModified = fileCache.getLastModified();
      // 检查是否需要返回304
      response = Resps.try304(request, lastModified);
      if (response != null) {
        response.addHeader(HeaderName.tio_from_cache, HeaderValue.Tio_From_Cache.TRUE);
        return response;
      }

      // 构建HttpResponse
      response = new HttpResponse(request);
      response.setBody(fileCache.getContent());

      response.setLastModified(HeaderValue.from(String.valueOf(lastModified)));
      response.setHasGzipped(fileCache.isHasGzipped());

      // 设置必要的响应头
      if (fileCache.getContentType() != null) {
        response.addHeader(HeaderName.Content_Type, fileCache.getContentType());
      }
      if (fileCache.getContentEncoding() != null) {
        response.addHeader(HeaderName.Content_Encoding, fileCache.getContentEncoding());
      }
      return response;
    } else {
      String pageRoot = httpConfig.getPageRoot(request);
      if (pageRoot != null) {
        HttpResource httpResource;
        try {
          httpResource = httpConfig.getResource(request, path);
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
        if (httpResource != null) {
          response = readFile(request, httpConfig, httpResource, staticResCache, enable);
          return response;
        }
      }
    }
    return response;
  }

  private HttpResponse readFile(HttpRequest request, HttpConfig httpConfig, HttpResource httpResource,
      //
      AbsCache staticResCache, boolean cacheFile) {
    HttpResponse response = null;
    String path = httpResource.getPath();
    File file = httpResource.getFile();
    String template = httpResource.getPath(); // "/res/css/header-all.css"
    InputStream inputStream = httpResource.getInputStream();

    String extension = FileUtil.extName(template);

    // 处理Freemarker模板
    FreemarkerConfig freemarkerConfig = httpConfig.getFreemarkerConfig();
    if (freemarkerConfig != null) {
      if (ArrayUtil.contains(freemarkerConfig.getSuffixes(), extension)) {
        Configuration configuration = freemarkerConfig.getConfiguration(request);
        if (configuration != null) {
          Object model = null;
          try {
            model = freemarkerConfig.getModelGenerator().generate(request);
          } catch (Exception e2) {
            e2.printStackTrace();
            return null;
          }
          if (request.isClosed()) {
            return null;
          } else {
            if (model instanceof HttpResponse) {
              response = (HttpResponse) model;
              return response;
            } else {
              try {
                String retStr = FreemarkerUtils.generateStringByPath(template, configuration, model);
                response = Resps.bytes(request, retStr.getBytes(configuration.getDefaultEncoding()), extension);
                return response;
              } catch (Throwable e) {
                try {
                  log.error("Freemarker error, treated as ordinary text processing：" + file.getCanonicalPath() + ", " + e.toString());
                } catch (IOException e1) {
                  e1.printStackTrace();
                }
              }
            }
          }
        }
      }
    }

    // 读取文件内容
    byte[] content = null;
    HeaderValue lastModified = null;
    long fileLastModified = 0;
    if (file != null) {
      fileLastModified = file.lastModified();
      content = FileUtil.readBytes(file);
      //lastModified = HeaderValue.getLastModifiedHeader(fileLastModified);
      lastModified = HeaderValue.from(String.valueOf(fileLastModified));
    } else {
      try {
        content = IoUtils.toByteArray(inputStream);
        fileLastModified = ManagementFactory.getRuntimeMXBean().getStartTime();
        lastModified = HeaderValue.getLastModifiedHeader(fileLastModified);
        lastModified = HeaderValue.from(String.valueOf(fileLastModified));
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }
    response = Resps.bytes(request, content, extension);
    response.setStaticRes(true);
    response.setLastModified(lastModified);

    // 缓存文件内容，如果文件大小小于最大缓存大小
    if (cacheFile && response.isStaticRes() && staticResCache != null) {
      if (response.getBody() != null && response.getStatus() == HttpResponseStatus.C200) {
        if (content.length <= MAX_CACHE_FILE_SIZE) {
          HeaderValue contentType = response.getHeader(HeaderName.Content_Type);
          HeaderValue contentEncoding = response.getHeader(HeaderName.Content_Encoding);
          FileCache newFileCache = new FileCache(content, fileLastModified, contentType, contentEncoding, response.hasGzipped());
          staticResCache.put(path, newFileCache);
          if (log.isInfoEnabled()) {
            log.info("add to cache:[{}], {}(B)", path, content.length);
          }
        } else {
          log.info("File size exceeds cache limit, not cached: [{}], {}(B)", path, content.length);
        }
      }
    }
    return response;
  }
}