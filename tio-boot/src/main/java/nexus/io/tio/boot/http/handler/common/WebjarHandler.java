package nexus.io.tio.boot.http.handler.common;

import java.net.URL;

import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.util.Resps;

public class WebjarHandler {
  public HttpResponse index(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    String uri = request.getRequestURI();
    String path = "META-INF/resources" + uri;
    URL resource = ResourceUtil.getResource(path);
    if (resource != null) {
      byte[] bytes = FileUtil.readBytes(resource);
      String extName = FileUtil.extName(uri);
      return Resps.bytes(response, bytes, extName);
    } else {
      response.setStatus(404);
      return response;
    }
  }
}
