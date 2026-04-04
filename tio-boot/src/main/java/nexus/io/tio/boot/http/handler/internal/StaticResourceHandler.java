package nexus.io.tio.boot.http.handler.internal;

import com.litongjava.tio.utils.cache.AbsCache;

import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

public interface StaticResourceHandler {
  public HttpResponse handle(String path, HttpRequest request, HttpConfig httpConfig, AbsCache staticResCache);
}
