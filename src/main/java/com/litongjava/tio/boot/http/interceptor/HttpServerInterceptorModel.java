package com.litongjava.tio.boot.http.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.tio.http.server.intf.HttpServerInterceptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HttpServerInterceptorModel {
  private String name;
  private List<String> allowedUrls; // 允许的地址
  private List<String> blockedUrls; // 被拦截的地址
  private HttpServerInterceptor interceptor;

  public HttpServerInterceptorModel addAllowedUrl(String string) {
    if (allowedUrls == null) {
      allowedUrls = new ArrayList<>();
    }
    allowedUrls.add(string);
    return this;
  }

  public HttpServerInterceptorModel addAllowedUrls(String... strings) {
    if (allowedUrls == null) {
      allowedUrls = new ArrayList<>();
    }
    for (String string : strings) {
      allowedUrls.add(string);
    }
    return this;
  }

  public HttpServerInterceptorModel addblockedUrl(String string) {
    if (blockedUrls == null) {
      blockedUrls = new ArrayList<>();
    }
    blockedUrls.add(string);
    return this;
  }

  public HttpServerInterceptorModel addBlockedUrls(String... strings) {
    if (blockedUrls == null) {
      blockedUrls = new ArrayList<>();
    }
    for (String string : strings) {
      blockedUrls.add(string);
    }
    return this;
  }
}
