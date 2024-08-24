package com.litongjava.tio.boot.http;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class TioRequestContext {
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

  public static void setUserId(Object userId) {
    requests.get().getRequest().setAttribute("userId", userId);
  }

  public static Object getUserId() {
    return requests.get().getRequest().getAttribute("userId");
  }

  public static String getUserIdString() {
    return (String) requests.get().getRequest().getAttribute("userId");
  }

  public static Long getUserIdLong() {
    return (Long) requests.get().getRequest().getAttribute("userId");
  }
}
