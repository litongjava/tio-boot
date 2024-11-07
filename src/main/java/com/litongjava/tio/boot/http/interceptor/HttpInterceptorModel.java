package com.litongjava.tio.boot.http.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
