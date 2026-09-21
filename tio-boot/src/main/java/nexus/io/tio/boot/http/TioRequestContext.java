package nexus.io.tio.boot.http;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

public class TioRequestContext {
  private static ThreadLocal<TioHttpAction> requests = new ThreadLocal<>();

  public static void hold(HttpRequest request, HttpResponse response) {
    requests.set(new TioHttpAction(request, response));
  }

  public static HttpRequest getRequest() {
    TioHttpAction tioHttpAction = requests.get();
    if (tioHttpAction != null) {
      return tioHttpAction.getRequest();
    }
    return null;

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

  public static void setIdentity(nexus.io.tio.boot.token.RequestIdentity identity) {
    getRequest().setAttribute(nexus.io.tio.boot.token.RequestIdentity.class.getName(), identity);
    setUserId(identity.getUserId());
  }

  public static nexus.io.tio.boot.token.RequestIdentity getIdentity() {
    HttpRequest request = getRequest();
    return request == null ? null : (nexus.io.tio.boot.token.RequestIdentity)
        request.getAttribute(nexus.io.tio.boot.token.RequestIdentity.class.getName());
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
