package nexus.io.tio.boot.context;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.boot.http.interceptor.*;
import nexus.io.tio.boot.http.handler.internal.TioBootHttpRequestDispatcher;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.http.common.*;
import nexus.io.tio.http.server.intf.HttpRequestInterceptor;
import nexus.io.tio.http.server.router.*;
import nexus.io.tio.http.server.handler.HttpRequestHandler;
import nexus.io.tio.utils.cache.mapcache.ConcurrentMapCacheFactory;

public class RouteInterceptorTest {
  private HttpRequest request(HttpMethod method) {
    HttpRequest r = new HttpRequest(); r.requestLine = new RequestLine();
    r.requestLine.setMethod(method); r.requestLine.setPath("/items"); r.requestLine.setVersion("1.1"); return r;
  }
  private HttpRequestInterceptor interceptor(List<String> calls, boolean reject) {
    return new HttpRequestInterceptor() {
      public HttpResponse doBeforeHandler(HttpRequest r, RequestLine l, HttpResponse p) { calls.add("before"); return null; }
      public HttpResponse doBeforeRoute(HttpRequest r, RequestLine l, HttpResponse p, RouteMatch m) {
        assertSame(r, TioRequestContext.getRequest()); calls.add("route");
        return reject ? new HttpResponse(r).setStatus(401) : null;
      }
      public void doAfterHandler(HttpRequest r, RequestLine l, HttpResponse p, long cost) {
        assertSame(r, TioRequestContext.getRequest()); calls.add("after");
      }
    };
  }
  private TioBootHttpRequestDispatcher dispatcher(DefaultHttpRequestRouter router, HttpRequestInterceptor interceptor,
      HttpRequestGroovyRouter groovy) {
    HttpConfig config = new HttpConfig(0, false); config.setUseSession(false);
    TioBootHttpRequestDispatcher d = new TioBootHttpRequestDispatcher();
    d.init(config, ConcurrentMapCacheFactory.INSTANCE, interceptor, null, router, groovy, null, null,
        null, r -> new HttpResponse(r).setStatus(404), null, null, (path, r, c, cache) -> null);
    return d;
  }
  @Test public void routeStageAndAfterHaveContextAndReleaseItOnSuccess() throws Exception {
    List<String> calls = new ArrayList<>(); DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", r -> { calls.add("handler"); return new HttpResponse(r); });
    assertEquals(200, dispatcher(router, interceptor(calls, false), null).handler(request(HttpMethod.GET)).getStatus().status);
    assertEquals(Arrays.asList("before", "route", "handler", "after"), calls);
    assertNull(TioRequestContext.getRequest());
  }
  @Test public void rejectionSkipsHandlerButAfterStillHasContext() throws Exception {
    List<String> calls = new ArrayList<>(); DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", r -> { fail("Unauthorized handler executed"); return null; });
    assertEquals(401, dispatcher(router, interceptor(calls, true), null).handler(request(HttpMethod.GET)).getStatus().status);
    assertEquals(Arrays.asList("before", "route", "after"), calls); assertNull(TioRequestContext.getRequest());
  }
  @Test public void methodMismatchLetsOtherDynamicRoutersHandlePath() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter(); router.get("/items", HttpResponse::new);
    HttpRequestGroovyRouter groovy = new HttpRequestGroovyRouter() {
      public void add(String path, HttpRequestHandler h) { }
      public HttpRequestHandler find(String path) { return r -> new HttpResponse(r).setStatus(201); }
    };
    assertEquals(201, dispatcher(router, interceptor(new ArrayList<>(), false), groovy)
        .handler(request(HttpMethod.POST)).getStatus().status);
    assertNull(TioRequestContext.getRequest());
  }
  @Test public void unnamedInterceptorsComposeAndMethodFilterApplies() throws Exception {
    HttpInteceptorConfigure previous = TioBootServer.me().getHttpInteceptorConfigure();
    try {
      HttpInteceptorConfigure config = new HttpInteceptorConfigure(); List<String> calls = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        HttpInterceptorModel model = new HttpInterceptorModel(); model.addBlockUrl("/**");
        model.setMethods(HttpMethod.POST); model.setInterceptor(interceptor(calls, false)); config.add(model);
      }
      assertEquals(2, config.getInteceptors().size()); TioBootServer.me().setHttpInteceptorConfigure(config);
      DefaultHttpRequestInterceptorDispatcher d = new DefaultHttpRequestInterceptorDispatcher();
      HttpRequest get = request(HttpMethod.GET); d.doBeforeHandler(get, get.requestLine, null); assertTrue(calls.isEmpty());
      HttpRequest post = request(HttpMethod.POST); d.doBeforeHandler(post, post.requestLine, null);
      assertEquals(Arrays.asList("before", "before"), calls);
    } finally { TioBootServer.me().setHttpInteceptorConfigure(previous); }
  }

  @Test public void failingAfterStillReleasesContext() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter(); router.get("/items", HttpResponse::new);
    HttpRequestInterceptor interceptor = new HttpRequestInterceptor() {
      public HttpResponse doBeforeHandler(HttpRequest r, RequestLine l, HttpResponse p) { return null; }
      public void doAfterHandler(HttpRequest r, RequestLine l, HttpResponse p, long cost) {
        assertSame(r, TioRequestContext.getRequest());
        throw new IllegalStateException("test after failure");
      }
    };
    assertEquals(200, dispatcher(router, interceptor, null).handler(request(HttpMethod.GET)).getStatus().status);
    assertNull(TioRequestContext.getRequest());
  }
}
