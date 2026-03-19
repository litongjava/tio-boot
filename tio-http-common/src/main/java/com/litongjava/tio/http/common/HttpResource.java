package com.litongjava.tio.http.common;

import java.io.File;
import java.net.URL;

/**
 */
public class HttpResource {
  private String path = null;
  private URL url = null;
  private File file = null;
  public String getPath() {
    return path;
  }
  public void setPath(String path) {
    this.path = path;
  }
  public URL getUrl() {
    return url;
  }
  public void setUrl(URL url) {
    this.url = url;
  }
  public File getFile() {
    return file;
  }
  public void setFile(File file) {
    this.file = file;
  }
  public HttpResource(String path, URL url, File file) {
    super();
    this.path = path;
    this.url = url;
    this.file = file;
  }
  public HttpResource() {
    super();
  }

}
