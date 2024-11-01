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
    String path = "META-INF/resources" + request.getRequestURI();
    URL resource = ResourceUtil.getResource(path);
    String html = null;
    if (resource != null) {
      html = FileUtil.readURLAsString(resource).toString();
      return Resps.html(response, html);
    } else {
      response.setStatus(404);
      return response;
    }
  }
}
