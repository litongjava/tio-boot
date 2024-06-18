package com.litongjava.tio.boot.http.handler;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

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
import com.litongjava.tio.utils.freemarker.FreemarkerUtils;
import com.litongjava.tio.utils.hutool.ArrayUtil;
import com.litongjava.tio.utils.hutool.FileUtil;

import freemarker.template.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StaticResourceHandler {
  
  private HttpConfig httpConfig;
  private AbsCache staticResCache;
  public StaticResourceHandler(HttpConfig httpConfig, AbsCache staticResCache) {
    // TODO Auto-generated constructor stub
  }

  public HttpResponse processStatic(String path, HttpRequest request) throws Exception {
    HttpResponse response = null;
    FileCache fileCache = null;
    File file = null;
    if (staticResCache != null) {
      // contentCache = CaffeineCache.getCache(STATIC_RES_CONTENT_CACHENAME);
      fileCache = (FileCache) staticResCache.get(path);
    }
    if (fileCache != null) {
      // byte[] bodyBytes = fileCache.getData();
      // Map<String, String> headers = fileCache.getHeaders();

      // HttpResponse responseInCache = fileCache.getResponse();

      long lastModified = fileCache.getLastModified();

      response = Resps.try304(request, lastModified);
      if (response != null) {
        response.addHeader(HeaderName.tio_from_cache, HeaderValue.Tio_From_Cache.TRUE);
        return response;
      }

      // response = fileCache.cloneResponse(request);
      response = fileCache.getResponse();
      response = HttpResponse.cloneResponse(request, response);

      // log.info("{}, 从缓存获取, 大小: {}", path, response.getBody().length);

      // response = new HttpResponse(request, httpConfig);
      // response.setBody(bodyBytes, request);
      // response.addHeaders(headers);
      return response;
    } else {
      String pageRoot = httpConfig.getPageRoot(request);
      if (pageRoot != null) {
        HttpResource httpResource = httpConfig.getResource(request, path);// .getFile(request, path);
        if (httpResource != null) {
          path = httpResource.getPath();
          file = httpResource.getFile();
          String template = httpResource.getPath(); // "/res/css/header-all.css"
          InputStream inputStream = httpResource.getInputStream();

          String extension = FileUtil.extName(template);

          // 项目中需要，时间支持一下freemarker模板，后面要做模板支持抽象设计
          FreemarkerConfig freemarkerConfig = httpConfig.getFreemarkerConfig();
          if (freemarkerConfig != null) {
            if (ArrayUtil.contains(freemarkerConfig.getSuffixes(), extension)) {
              Configuration configuration = freemarkerConfig.getConfiguration(request);
              if (configuration != null) {
                Object model = freemarkerConfig.getModelGenerator().generate(request);
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
                      log.error("freemarker错误，当成普通文本处理：" + file.getCanonicalPath() + ", " + e.toString());
                    }
                  }
                }
              }
            }
          }

          if (file != null) {
            response = Resps.file(request, file);
          } else {
            response = Resps.bytes(request, IoUtils.toByteArray(inputStream), extension);// .file(request, file);
          }

          response.setStaticRes(true);

          // 把静态资源放入缓存
          if (response.isStaticRes() && staticResCache != null/** && request.getIsSupportGzip()*/
          ) {
            if (response.getBody() != null && response.getStatus() == HttpResponseStatus.C200) {
              HeaderValue contentType = response.getHeader(HeaderName.Content_Type);
              HeaderValue contentEncoding = response.getHeader(HeaderName.Content_Encoding);
              HeaderValue lastModified = response.getLastModified();// .getHeader(HttpConst.ResponseHeaderKey.Last_Modified);

              Map<HeaderName, HeaderValue> headers = new HashMap<>();
              if (contentType != null) {
                headers.put(HeaderName.Content_Type, contentType);
              }
              if (contentEncoding != null) {
                headers.put(HeaderName.Content_Encoding, contentEncoding);
              }

              HttpResponse responseInCache = new HttpResponse(request);
              responseInCache.addHeaders(headers);
              if (lastModified != null) {
                responseInCache.setLastModified(lastModified);
              }
              responseInCache.setBody(response.getBody());
              responseInCache.setHasGzipped(response.isHasGzipped());

              if (file != null) {
                fileCache = new FileCache(responseInCache, file.lastModified());
              } else {
                fileCache = new FileCache(responseInCache, ManagementFactory.getRuntimeMXBean().getStartTime());
              }

              staticResCache.put(path, fileCache);
              if (log.isInfoEnabled()) {
                log.info("add to cache:[{}], {}(B)", path, response.getBody().length);
              }
            }
          }
          return response;
        }
      }
    }
    return response;
  }

}
