package com.litongjava.tio.boot.http.handler.common;

import java.net.URL;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

public class WebjarHandler {
  public HttpResponse index(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    String uri = request.getRequestURI();
    String path = "META-INF/resources" + uri;
    URL resource = ResourceUtil.getResource(path);
    if (resource != null) {
      byte[] bytes = FileUtil.readUrlAsBytes(resource);
      String extName = FileUtil.extName(uri);
      return Resps.bytes(response, bytes, extName);
    } else {
      response.setStatus(404);
      return response;
    }
  }
}
