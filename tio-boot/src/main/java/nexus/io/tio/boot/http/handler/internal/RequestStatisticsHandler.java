package nexus.io.tio.boot.http.handler.internal;

import nexus.io.tio.http.common.HttpRequest;

@FunctionalInterface
public interface RequestStatisticsHandler {

  void count(HttpRequest request);
}
