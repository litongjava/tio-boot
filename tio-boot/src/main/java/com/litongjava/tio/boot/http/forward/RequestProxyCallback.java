package com.litongjava.tio.boot.http.forward;

import java.util.Map;

import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;

public interface RequestProxyCallback {

  public void saveRequest(long id, String ip, HttpRequest httpRequest);

  public void saveResponse(long id, long elapsed, int statusCode, Map<HeaderName, HeaderValue> headers,
      HeaderValue contentEncoding, byte[] body);

}
