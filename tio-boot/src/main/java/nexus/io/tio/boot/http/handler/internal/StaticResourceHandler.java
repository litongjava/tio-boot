package nexus.io.tio.boot.http.handler.internal;

import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.utils.cache.AbsCache;

public interface StaticResourceHandler {
  public HttpResponse handle(String path, HttpRequest request, HttpConfig httpConfig, AbsCache staticResCache);
}
