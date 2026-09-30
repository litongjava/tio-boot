package nexus.io.tio.http.server;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import nexus.io.tio.http.common.*;
import nexus.io.tio.http.server.handler.*;
import nexus.io.tio.http.server.router.*;

public class MethodRouterTest {
  private final HttpRequestHandler get = r -> new HttpResponse(r).body("get");
  private final HttpRequestHandler post = r -> new HttpResponse(r).body("post");

  private String wire(HttpResponse response) {
    ByteBuffer buffer = HttpResponseEncoder.encode(response, null, null);
    byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
  }

  @Test public void optionsStarDiscoversServerMethodsWithoutRunningHandlers() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.post("/login", r -> { fail("Business handler invoked"); return null; });
    HttpResponse response = new DefaultHttpRequestDispatcher(null, router).handler(request(HttpMethod.OPTIONS, "*"));
    assertEquals(204, response.getStatus().status);
    String encoded = wire(response);
    assertTrue(encoded.contains("allow:post, options"));
    assertFalse(encoded.contains("content-length:"));
    assertTrue(encoded.endsWith("\r\n\r\n"));
    assertNull(router.automaticOptions(request(HttpMethod.OPTIONS, "/missing")));
    assertEquals(RouteMatch.Status.METHOD_NOT_ALLOWED, router.match(request(HttpMethod.HEAD, "/login")).getStatus());
  }

  @Test public void headPreservesGetMetadataAndExplicitHeadCanSkipGet() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", r -> new HttpResponse(r).body("hello").addHeader("ETag", "v1"));
    DefaultHttpRequestDispatcher dispatcher = new DefaultHttpRequestDispatcher(null, router);
    String encoded = wire(dispatcher.handler(request(HttpMethod.HEAD, "/items")));
    assertTrue(encoded.contains("etag:v1"));
    assertTrue(encoded.contains("content-length:5"));
    assertFalse(encoded.contains("hello"));
    router.head("/items", r -> new HttpResponse(r).addHeader("Content-Length", "123").addHeader("ETag", "v2"));
    encoded = wire(dispatcher.handler(request(HttpMethod.HEAD, "/items")));
    assertTrue(encoded.contains("etag:v2"));
    assertTrue(encoded.contains("content-length:123"));
    assertEquals(1, encoded.split("content-length:", -1).length - 1);
    assertFalse(wire(new HttpResponse(request(HttpMethod.HEAD, "/items"))).contains("content-length:"));
  }

  @Test public void noContentStatusesNeverSendPayloadOrInvalidLength() {
    for (int status : Arrays.asList(100, 204, 304)) {
      HttpResponse response = new HttpResponse(request(HttpMethod.OPTIONS, "/items"));
      response.setStatus(status); response.body("forbidden");
      if (status != 304) response.addHeader("Content-Length", "9").addHeader("Transfer-Encoding", "chunked");
      String encoded = wire(response);
      assertFalse(encoded.contains("content-length:"));
      assertFalse(encoded.contains("transfer-encoding:"));
      assertFalse(encoded.contains("forbidden"));
      assertTrue(encoded.endsWith("\r\n\r\n"));
    }
  }

  private HttpRequest request(HttpMethod method, String path) {
    HttpRequest r = new HttpRequest();
    r.requestLine = new RequestLine();
    r.requestLine.setMethod(method);
    r.requestLine.setPath(path);
    r.requestLine.setVersion("1.1");
    return r;
  }

  @Test public void methodsCoexistAndDuplicateFails() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/items", get); router.post("/items", post);
    assertSame(get, router.resolve(request(HttpMethod.GET, "/items")));
    assertSame(post, router.resolve(request(HttpMethod.POST, "/items")));
    assertThrows(IllegalArgumentException.class, () -> router.get("/items", post));
    assertEquals(2, router.allRoutes().size());
  }

  @Test public void notFoundAndMethodErrorAreDifferent() throws Exception {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter(); router.get("/items", get);
    assertEquals(RouteMatch.Status.NOT_FOUND, router.match(request(HttpMethod.POST, "/missing")).getStatus());
    HttpRequest r = request(HttpMethod.POST, "/items");
    RouteMatch match = router.match(r);
    assertEquals(RouteMatch.Status.METHOD_NOT_ALLOWED, match.getStatus());
    assertEquals(new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)), match.getAllowedMethods());
    assertEquals(405, new DefaultHttpRequestDispatcher(null, router).handler(r).getStatus().status);
    assertEquals(204, new DefaultHttpRequestDispatcher(null, router).handler(request(HttpMethod.OPTIONS, "/items")).getStatus().status);
  }

  @Test public void defaultMethodsWorkAndExplicitMethodsWin() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.add("/items", get); router.post("/items", post);
    assertSame(get, router.find("/items"));
    assertSame(get, router.resolve(request(HttpMethod.DELETE, "/items")));
    assertSame(post, router.resolve(request(HttpMethod.POST, "/items")));
    router.add("/items", post); assertSame(post, router.find("/items"));
  }

  @Test public void defaultRegistrationsExcludeOptionsAndPatchForEveryPathType() throws Exception {
    for (String pattern : Arrays.asList("/items/42", "/items/*", "/items/**", "/items/{id}")) {
      DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
      router.add(pattern, get);
      assertEquals(4, router.allRoutes().size());
      for (HttpMethod method : Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD))
        assertSame(get, router.resolve(request(method, "/items/42")));
      assertNull(router.resolve(request(HttpMethod.OPTIONS, "/items/42")));
      assertNull(router.resolve(request(HttpMethod.PATCH, "/items/42")));
      RouteMatch match = router.match(request(HttpMethod.OPTIONS, "/items/42"));
      assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE,
          HttpMethod.HEAD, HttpMethod.OPTIONS), match.getAllowedMethods());
      DefaultHttpRequestDispatcher dispatcher = new DefaultHttpRequestDispatcher(null, router);
      assertEquals(204, dispatcher.handler(request(HttpMethod.OPTIONS, "/items/42")).getStatus().status);
      assertEquals(405, dispatcher.handler(request(HttpMethod.PATCH, "/items/42")).getStatus().status);
      router.options(pattern, post);
      assertSame(post, router.resolve(request(HttpMethod.OPTIONS, "/items/42")));
      assertThrows(IllegalArgumentException.class, () -> router.options(pattern, get));
    }
  }

  @Test public void defaultsDoNotReplaceExplicitRegistrationsInEitherOrder() {
    for (boolean defaultsFirst : Arrays.asList(true, false)) {
      DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
      if (defaultsFirst) router.add("/items", get);
      router.post("/items", post); router.options("/items", post);
      router.add("/items", get); router.add("/items", get);
      assertSame(post, router.resolve(request(HttpMethod.POST, "/items")));
      assertSame(post, router.resolve(request(HttpMethod.OPTIONS, "/items")));
      assertSame(get, router.resolve(request(HttpMethod.GET, "/items")));
      assertEquals(6, router.allRoutes().size());
    }
  }

  @Test public void templatesDoNotPolluteFailedMatchesAndMetadataFollowsMethod() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.add(HttpMethod.GET, "/items/{id:[0-9]+}", get, Collections.singletonMap("access", "public"));
    router.add(HttpMethod.POST, "/items/{id:[0-9]+}", post, Collections.singletonMap("access", "user"));
    HttpRequest r = request(HttpMethod.POST, "/items/42"); RouteMatch match = router.match(r);
    assertNull(r.getParam("id")); assertEquals("42", match.getParameters().get("id"));
    assertEquals("user", match.getRoute().getMetadata().get("access"));
    r.addParam("id", "999");
    match.apply(r); assertEquals("42", r.getParam("id"));
    HttpRequest wrong = request(HttpMethod.DELETE, "/items/42"); router.resolve(wrong); assertNull(wrong.getParam("id"));
    assertEquals(RouteMatch.Status.NOT_FOUND, router.match(request(HttpMethod.GET, "/items/no")).getStatus());
  }

  @Test public void optionalAndVariableFirstTemplatesAreNotHiddenByOtherCandidates() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.get("/x/{a}/{b}?", get); router.get("/x/{a}", post); router.post("/{root}/fixed", post);
    assertSame(get, router.resolve(request(HttpMethod.GET, "/x/1")));
    assertSame(post, router.resolve(request(HttpMethod.POST, "/x/fixed")));
  }

  @Test public void registeringAfterLookupTakesEffectImmediately() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter(); router.get("/items/{id}", get);
    assertSame(get, router.resolve(request(HttpMethod.GET, "/items/42")));
    router.get("/items/42", post);
    assertSame(post, router.resolve(request(HttpMethod.GET, "/items/42")));
  }

  @Test public void headFallsBackToGetAndExplicitHeadWins() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter(); router.get("/items", get);
    assertSame(get, router.resolve(request(HttpMethod.HEAD, "/items")));
    router.add(HttpMethod.HEAD, "/items", post);
    assertSame(post, router.resolve(request(HttpMethod.HEAD, "/items")));
  }

  @Test public void headEncoderKeepsLengthButDoesNotWriteBody() {
    HttpResponse response = new HttpResponse(request(HttpMethod.HEAD, "/items")).body("hello");
    ByteBuffer buffer = HttpResponseEncoder.encode(response, null, null);
    byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes);
    String encoded = new String(bytes, StandardCharsets.UTF_8);
    assertTrue(encoded.toLowerCase().contains("content-length:5"));
    assertTrue(encoded.endsWith("\r\n\r\n")); assertFalse(encoded.contains("hello"));
  }

  @Test public void headFileResponseDoesNotReachFileTransfer() throws Exception {
    java.io.File file = java.io.File.createTempFile("tio-head-", ".txt");
    try {
      java.nio.file.Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
      HttpResponse response = new HttpResponse(request(HttpMethod.HEAD, "/file"));
      response.setFileBody(file);
      response.setFileBodyLength(5);
      ByteBuffer buffer = HttpResponseEncoder.encode(response, null, null);
      byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes);
      assertTrue(new String(bytes, StandardCharsets.UTF_8).toLowerCase().contains("content-length:5"));
      assertNull(response.getFileBody());
      HttpResponse noContent = new HttpResponse(request(HttpMethod.OPTIONS, "/file"));
      noContent.setStatus(204); noContent.setFileBody(file); noContent.setFileBodyLength(5);
      assertFalse(wire(noContent).contains("content-length:"));
      assertNull(noContent.getFileBody());
    } finally { java.nio.file.Files.deleteIfExists(file.toPath()); }
  }
}
