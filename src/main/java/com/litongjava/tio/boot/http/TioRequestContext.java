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
    requests.get().getRequest().setAttribute("userId", userId);
  }

  public static Object getUserId() {
    return requests.get().getRequest().getAttribute("userId");
  }

  public static String getUserIdString() {
    Object attribute = requests.get().getRequest().getAttribute("userId");
    if (attribute != null) {
      if (attribute instanceof String) {
        return (String) attribute;
      } else {
        return attribute.toString();
      }
    }
    return null;

  }

  public static Long getUserIdLong() {
    Object attribute = requests.get().getRequest().getAttribute("userId");
    if (attribute != null) {
      if (attribute instanceof Long) {
        return (Long) attribute;
      } else {
        return Long.valueOf((String) attribute);
      }
    }
    return null;

  }

  public static ChannelContext getChannelContext() {
    return getRequest().getChannelContext();
  }
}
