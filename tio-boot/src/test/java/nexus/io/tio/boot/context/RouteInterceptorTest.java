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
  @Test
  public void defaultOptionsNeverCallsBusinessAndExplicitOptionsHandlesPreflight() throws Exception {
    for (boolean cors : Arrays.asList(true, false)) {
      List<String> calls = new ArrayList<>();
      DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
      router.add("/items", r -> {
        fail("OPTIONS reached business handler");
        return null;
      });
      TioBootHttpRequestDispatcher d = dispatcher(router, interceptor(calls, false), null);
      java.lang.reflect.Field field = TioBootHttpRequestDispatcher.class.getDeclaredField("corsEnable");
      field.setAccessible(true);
      field.setBoolean(d, cors);
      HttpRequest preflight = request(HttpMethod.OPTIONS);
      preflight.addHeader("origin", "https://example.com");
      preflight.addHeader("access-control-request-method", "POST");
      HttpResponse response = d.handler(preflight);
      assertEquals(204, response.getStatus().status);
      if (cors)
        assertEquals(response.getHeader(HeaderName.from("Allow")).toString(),
            response.getHeader(HeaderName.Access_Control_Allow_Methods).toString());
      assertTrue(calls.isEmpty());
      assertEquals(204, d.handler(request(HttpMethod.OPTIONS)).getStatus().status);
      assertTrue(calls.isEmpty());
      calls.clear();
      router.options("/items", r -> {
        calls.add("options");
        return new HttpResponse(r).setStatus(202);
      });
      assertEquals(202, d.handler(preflight).getStatus().status);
      assertEquals(Arrays.asList("before", "route", "options", "after"), calls);
      assertNull(TioRequestContext.getRequest());
    }
  }

  private HttpRequest request(HttpMethod method) {
    HttpRequest r = new HttpRequest();
    r.requestLine = new RequestLine();
    r.requestLine.setMethod(method);
    r.requestLine.setPath("/items");
    r.requestLine.setVersion("1.1");
    return r;
  }

  private HttpRequestInterceptor interceptor(List<String> calls, boolean reject) {
    return new HttpRequestInterceptor() {
      public HttpResponse doBeforeHandler(HttpRequest r, RequestLine l, HttpResponse p) {
        calls.add("before");
        return null;
      }

      public HttpResponse doBeforeRoute(HttpRequest r, RequestLine l, HttpResponse p, RouteMatch m) {
        assertSame(r, TioRequestContext.getRequest());
        calls.add("route");
        return reject ? new HttpResponse(r).setStatus(401) : null;
      }

      public void doAfterHandler(HttpRequest r, RequestLine l, HttpResponse p, long cost) {
        assertSame(r, TioRequestContext.getRequest());
        calls.add("after");
      }
    };
  }

  private TioBootHttpRequestDispatcher dispatcher(DefaultHttpRequestRouter router, HttpRequestInterceptor interceptor,
      HttpRequestGroovyRouter groovy) {
    HttpConfig config = new HttpConfig(0, false);
    config.setUseSession(false);
    TioBootHttpRequestDispatcher d = new TioBootHttpRequestDispatcher();
    d.init(config, ConcurrentMapCacheFactory.INSTANCE, interceptor, null, router, groovy, null, null, null,
        r -> new HttpResponse(r).setStatus(404), null, null, (path, r, c, cache) -> null);
    return d;
  }

  @Test
  public void routeStageAndAfterHaveContextAndReleaseItOnSuccess() throws Exception {
    List<String> calls = new ArrayList<>();
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", r -> {
      calls.add("handler");
      return new HttpResponse(r);
    });
    assertEquals(200,
        dispatcher(router, interceptor(calls, false), null).handler(request(HttpMethod.GET)).getStatus().status);
    assertEquals(Arrays.asList("before", "route", "handler", "after"), calls);
    assertNull(TioRequestContext.getRequest());
  }

  @Test
  public void rejectionSkipsHandlerButAfterStillHasContext() throws Exception {
    List<String> calls = new ArrayList<>();
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", r -> {
      fail("Unauthorized handler executed");
      return null;
    });
    assertEquals(401,
        dispatcher(router, interceptor(calls, true), null).handler(request(HttpMethod.GET)).getStatus().status);
    assertEquals(Arrays.asList("before", "route", "after"), calls);
    assertNull(TioRequestContext.getRequest());
  }

  @Test
  public void methodMismatchLetsOtherDynamicRoutersHandlePath() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", HttpResponse::new);
    HttpRequestGroovyRouter groovy = new HttpRequestGroovyRouter() {
      public void add(String path, HttpRequestHandler h) {
      }

      public HttpRequestHandler find(String path) {
        return r -> new HttpResponse(r).setStatus(201);
      }
    };
    assertEquals(201, dispatcher(router, interceptor(new ArrayList<>(), false), groovy)
        .handler(request(HttpMethod.POST)).getStatus().status);
    assertNull(TioRequestContext.getRequest());
  }

  @Test
  public void unnamedInterceptorsComposeAndMethodFilterApplies() throws Exception {
    HttpInteceptorConfigure previous = TioBootServer.me().getHttpInteceptorConfigure();
    try {
      HttpInteceptorConfigure config = new HttpInteceptorConfigure();
      List<String> calls = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        HttpInterceptorModel model = new HttpInterceptorModel();
        model.addBlockUrl("/**");
        model.setMethods(HttpMethod.POST);
        model.setInterceptor(interceptor(calls, false));
        config.add(model);
      }
      assertEquals(2, config.getInteceptors().size());
      TioBootServer.me().setHttpInteceptorConfigure(config);
      DefaultHttpRequestInterceptorDispatcher d = new DefaultHttpRequestInterceptorDispatcher();
      HttpRequest get = request(HttpMethod.GET);
      d.doBeforeHandler(get, get.requestLine, null);
      assertTrue(calls.isEmpty());
      HttpRequest post = request(HttpMethod.POST);
      d.doBeforeHandler(post, post.requestLine, null);
      assertEquals(Arrays.asList("before", "before"), calls);
    } finally {
      TioBootServer.me().setHttpInteceptorConfigure(previous);
    }
  }

  @Test
  public void optionsStarAndGlobalPreflightAreAutomaticButMissingGetIsNotSuccessful() throws Exception {
    List<String> calls = new ArrayList<>();
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.post("/items", r -> {
      fail("OPTIONS invoked POST");
      return null;
    });
    TioBootHttpRequestDispatcher d = dispatcher(router, interceptor(calls, false), null);
    java.lang.reflect.Field field = TioBootHttpRequestDispatcher.class.getDeclaredField("corsEnable");
    field.setAccessible(true);
    field.setBoolean(d, true);
    HttpRequest star = request(HttpMethod.OPTIONS);
    star.requestLine.setPath("*");
    assertEquals(204, d.handler(star).getStatus().status);
    assertTrue(calls.isEmpty());
    HttpRequest missing = request(HttpMethod.OPTIONS);
    missing.requestLine.setPath("/missing");
    missing.addHeader("origin", "https://example.com");
    missing.addHeader("access-control-request-method", "POST");
    assertEquals(204, d.handler(missing).getStatus().status);
    assertTrue(calls.isEmpty());
    missing.requestLine.setMethod(HttpMethod.GET);
    assertEquals(404, d.handler(missing).getStatus().status);
    assertNull(TioRequestContext.getRequest());
  }

  @Test
  public void failingAfterStillReleasesContext() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", HttpResponse::new);
    HttpRequestInterceptor interceptor = new HttpRequestInterceptor() {
      public HttpResponse doBeforeHandler(HttpRequest r, RequestLine l, HttpResponse p) {
        return null;
      }

      public void doAfterHandler(HttpRequest r, RequestLine l, HttpResponse p, long cost) {
        assertSame(r, TioRequestContext.getRequest());
        throw new IllegalStateException("test after failure");
      }
    };
    assertEquals(200, dispatcher(router, interceptor, null).handler(request(HttpMethod.GET)).getStatus().status);
    assertNull(TioRequestContext.getRequest());
  }

  @Test
  public void globalPreflightBypassesAuthenticationButActualRequestAndDisabledCorsDoNot() throws Exception {
    List<String> calls = new ArrayList<>();
    HttpRequestInterceptor auth = new HttpRequestInterceptor() {
      public HttpResponse doBeforeHandler(HttpRequest request, RequestLine line, HttpResponse response) {
        calls.add("auth");
        return new HttpResponse(request).setStatus(401);
      }

      public void doAfterHandler(HttpRequest request, RequestLine line, HttpResponse response, long cost) {
      }
    };
    // Controller/Function paths are not registered in the method router.
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    TioBootHttpRequestDispatcher dispatcher = dispatcher(router, auth, null);
    java.lang.reflect.Field field = TioBootHttpRequestDispatcher.class.getDeclaredField("corsEnable");
    field.setAccessible(true);
    field.setBoolean(dispatcher, true);
    HttpRequest preflight = request(HttpMethod.OPTIONS);
    preflight.requestLine.setPath("/api/user");
    preflight.addHeader("origin", "https://cloud.example.com");
    preflight.addHeader("access-control-request-method", "GET");
    preflight.addHeader("access-control-request-headers", "authorization");
    HttpResponse response = dispatcher.handler(preflight);
    assertEquals(204, response.getStatus().status);
    assertEquals("authorization", response.getHeader(HeaderName.Access_Control_Allow_Headers).toString());
    assertEquals("https://cloud.example.com", response.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
    assertTrue(calls.isEmpty());
    assertNull(TioRequestContext.getRequest());
    preflight.requestLine.setMethod(HttpMethod.GET);
    assertEquals(401, dispatcher.handler(preflight).getStatus().status);
    assertEquals(1, calls.size());
    preflight.requestLine.setMethod(HttpMethod.OPTIONS);
    field.setBoolean(dispatcher, false);
    response = dispatcher.handler(preflight);
    assertEquals(401, response.getStatus().status);
    assertNull(response.getHeader(HeaderName.Access_Control_Allow_Origin));
    assertEquals(2, calls.size());
    field.setBoolean(dispatcher, true);
    assertEquals(401, dispatcher.handler(request(HttpMethod.OPTIONS)).getStatus().status);
    router.options("/api/user", request -> {
      fail("Unauthorized explicit OPTIONS handler executed");
      return null;
    });
    assertEquals(401, dispatcher.handler(preflight).getStatus().status);
    assertEquals(4, calls.size());
    assertNull(TioRequestContext.getRequest());
  }
}
