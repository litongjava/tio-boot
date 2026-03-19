package com.litongjava.tio.http.multipart;

import java.nio.charset.StandardCharsets;

public class TioMultipartPart {
  private final String name;
  private final String contentType;
  private final byte[] payload;

  private TioMultipartPart(String name, String contentType, byte[] payload) {
    this.name = name;
    this.contentType = contentType;
    this.payload = payload;
  }

  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getPayload() {
    return payload;
  }

  public static TioMultipartPart text(String name, String value, String contentType) {
    byte[] payload = value.getBytes(StandardCharsets.UTF_8);
    return new TioMultipartPart(name, contentType, payload);
  }
}