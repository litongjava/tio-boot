package nexus.io.tio.http.multipart;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpResponse;

public class TioMultipartHttpResponder {

  private final TioMultipartEncoder encoder = new TioMultipartEncoder();

  public void respondFormData(HttpResponse response, TioMultipartParts parts) throws Exception {
    TioMultipartBody body = encoder.encode(parts);
    response.setStatus(200);
    response.addHeader(HeaderName.Content_Type, HeaderValue.from(body.contentTypeFormData()));
    response.setBody(body.getBodyBytes());
  }

  public void respondMixed(HttpResponse response, TioMultipartParts parts) throws Exception {
    TioMultipartBody body = encoder.encode(parts);
    response.setStatus(200);
    response.addHeader(HeaderName.Content_Type, HeaderValue.from(body.contentTypeMixed()));
    response.setBody(body.getBodyBytes());
  }
}
