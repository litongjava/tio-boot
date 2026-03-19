package com.litongjava.tio.http.server.handler;

import java.io.Serializable;

import com.litongjava.tio.http.common.HeaderValue;

public class FileCache implements Serializable {

  private static final long serialVersionUID = 6517890350387789902L;

  private long lastModified;
  private byte[] content;
  private HeaderValue contentType;
  private HeaderValue contentEncoding;
  private boolean hasGzipped;

  public FileCache() {
  }

  public FileCache(byte[] content, long lastModified, HeaderValue contentType, HeaderValue contentEncoding, boolean hasGzipped) {
    this.content = content;
    this.lastModified = lastModified;
    this.contentType = contentType;
    this.contentEncoding = contentEncoding;
    this.hasGzipped = hasGzipped;
  }

  public long getLastModified() {
    return lastModified;
  }

  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }

  public HeaderValue getContentType() {
    return contentType;
  }

  public void setContentType(HeaderValue contentType) {
    this.contentType = contentType;
  }

  public HeaderValue getContentEncoding() {
    return contentEncoding;
  }

  public void setContentEncoding(HeaderValue contentEncoding) {
    this.contentEncoding = contentEncoding;
  }

  public boolean isHasGzipped() {
    return hasGzipped;
  }

  public void setHasGzipped(boolean hasGzipped) {
    this.hasGzipped = hasGzipped;
  }
}