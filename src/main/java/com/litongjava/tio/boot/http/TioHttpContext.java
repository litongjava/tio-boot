package com.litongjava.tio.boot.http;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class TioHttpContext {
  private static ThreadLocal<TioHttpAction> requests = new ThreadLocal<>();

  public static void hold(HttpRequest request) {
    HttpResponse response = new HttpResponse(request);
    requests.set(new TioHttpAction(request, response));
  }

  public static HttpRequest getRequest() {
    return requests.get().getRequest();
  }

  public static void release() {
    requests.remove();
  }

  public static HttpResponse getResponse() {
    return requests.get().getResponse();
  }

  public static void setUserId(String userId) {
    requests.get().getRequest().setAttribute("userId", userId);
  }

  public static String getUserId() {
    return (String) requests.get().getRequest().getAttribute("userId");
  }

  public static void setUserIdLong(Long userId) {
    requests.get().getRequest().setAttribute("userId", userId);
  }

  public static Long getUserIdLong() {
    return (Long) requests.get().getRequest().getAttribute("userId");
  }
}
