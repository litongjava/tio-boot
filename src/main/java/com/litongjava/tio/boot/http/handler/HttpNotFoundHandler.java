package com.litongjava.tio.boot.http.handler;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public interface HttpNotFoundHandler {

  HttpResponse handle(HttpRequest request);

}
