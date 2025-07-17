package com.litongjava.tio.boot.http;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class TioRequestContext {
  private static ThreadLocal<TioHttpAction> requests = new ThreadLocal<>();

  public static void hold(HttpRequest request, HttpResponse response) {
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
    requests.get().getRequest().setUserId(userId);
  }

  public static Object getUserId() {
    return requests.get().getRequest().getUserId();
  }

  public static String getUserIdString() {
    return requests.get().getRequest().getUserIdString();
  }

  public static Long getUserIdLong() {
    return requests.get().getRequest().getUserIdLong();
  }

  public static ChannelContext getChannelContext() {
    return getRequest().getChannelContext();
  }
}
