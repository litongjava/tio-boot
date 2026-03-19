package com.litongjava.tio.boot.http.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;


public class HttpInterceptorModel {
  private String name;
  private boolean alloweStaticFile;
  private List<String> allowedUrls;
  private List<String> blockedUrls;
  private HttpRequestInterceptor interceptor;

  public HttpInterceptorModel addAllowUrl(String string) {
    if (allowedUrls == null) {
      allowedUrls = new ArrayList<>();
    }
    allowedUrls.add(string);
    return this;
  }

  public HttpInterceptorModel addAllowUrls(String... strings) {
    if (allowedUrls == null) {
      allowedUrls = new ArrayList<>();
    }
    for (String string : strings) {
      allowedUrls.add(string);
    }
    return this;
  }

  public HttpInterceptorModel addBlockUrl(String string) {
    if (blockedUrls == null) {
      blockedUrls = new ArrayList<>();
    }
    blockedUrls.add(string);
    return this;
  }

  public HttpInterceptorModel addBlockeUrls(String... strings) {
    if (blockedUrls == null) {
      blockedUrls = new ArrayList<>();
    }
    for (String string : strings) {
      blockedUrls.add(string);
    }
    return this;
  }

  public HttpInterceptorModel() {
    super();
    // TODO Auto-generated constructor stub
  }

  public HttpInterceptorModel(String name, boolean alloweStaticFile, List<String> allowedUrls, List<String> blockedUrls,
      HttpRequestInterceptor interceptor) {
    super();
    this.name = name;
    this.alloweStaticFile = alloweStaticFile;
    this.allowedUrls = allowedUrls;
    this.blockedUrls = blockedUrls;
    this.interceptor = interceptor;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isAlloweStaticFile() {
    return alloweStaticFile;
  }

  public void setAlloweStaticFile(boolean alloweStaticFile) {
    this.alloweStaticFile = alloweStaticFile;
  }

  public List<String> getAllowedUrls() {
    return allowedUrls;
  }

  public void setAllowedUrls(List<String> allowedUrls) {
    this.allowedUrls = allowedUrls;
  }

  public List<String> getBlockedUrls() {
    return blockedUrls;
  }

  public void setBlockedUrls(List<String> blockedUrls) {
    this.blockedUrls = blockedUrls;
  }

  public HttpRequestInterceptor getInterceptor() {
    return interceptor;
  }

  public void setInterceptor(HttpRequestInterceptor interceptor) {
    this.interceptor = interceptor;
  }
}
