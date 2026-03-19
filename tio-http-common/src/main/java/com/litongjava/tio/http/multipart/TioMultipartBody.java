package com.litongjava.tio.http.multipart;

public class TioMultipartBody {
  private final String boundary;
  private final byte[] bodyBytes;

  public TioMultipartBody(String boundary, byte[] bodyBytes) {
    this.boundary = boundary;
    this.bodyBytes = bodyBytes;
  }

  public String getBoundary() {
    return boundary;
  }

  public byte[] getBodyBytes() {
    return bodyBytes;
  }

  public String contentTypeFormData() {
    return "multipart/form-data; boundary=" + boundary;
  }

  public String contentTypeMixed() {
    return "multipart/mixed; boundary=" + boundary;
  }
}
