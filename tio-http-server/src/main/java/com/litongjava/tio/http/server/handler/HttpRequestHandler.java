package com.litongjava.tio.http.server.handler;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

@FunctionalInterface
public interface HttpRequestHandler {
  HttpResponse handle(HttpRequest httpRequest) throws Exception;
}
