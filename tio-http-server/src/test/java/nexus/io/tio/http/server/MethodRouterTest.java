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

  @Test public void legacyAnyStillWorksAndExplicitMethodsWin() {
    DefaultHttpRequestRouter router = new DefaultHttpRequestRouter();
    router.add("/items", get); router.post("/items", post);
    assertSame(get, router.find("/items"));
    assertSame(get, router.resolve(request(HttpMethod.DELETE, "/items")));
    assertSame(post, router.resolve(request(HttpMethod.POST, "/items")));
    router.add("/items", post); assertSame(post, router.find("/items"));
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
    } finally { java.nio.file.Files.deleteIfExists(file.toPath()); }
  }
}
