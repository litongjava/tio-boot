package nexus.io.tio.boot.http.forward;

import java.util.Map;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpRequest;

public interface RequestProxyCallback {

  public void saveRequest(long id, String ip, HttpRequest httpRequest);

  public void saveResponse(long id, long elapsed, int statusCode, Map<HeaderName, HeaderValue> headers,
      HeaderValue contentEncoding, byte[] body);

}
