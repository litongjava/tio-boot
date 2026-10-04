package nexus.io.tio.utils.http;

import static org.junit.Assert.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.Test;
import com.sun.net.httpserver.HttpServer;
import nexus.io.model.http.response.ResponseVo;
import nexus.io.tio.utils.IoUtils;
import okhttp3.Response;

public class HttpClientResponseTest {
  @Test
  public void uploadUsesFilenameExtensionForMultipartType() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    Path file = Files.createTempFile("http-upload-", ".PNG");
    AtomicReference<String> multipart = new AtomicReference<>();
    server.createContext("/", exchange -> {
      multipart.set(new String(IoUtils.toByteArray(exchange.getRequestBody()), StandardCharsets.UTF_8));
      exchange.sendResponseHeaders(204, -1);
      exchange.close();
    });
    server.start();
    try {
      Files.write(file, new byte[] {1, 2});
      assertTrue(HttpUtils.upload("http://127.0.0.1:" + server.getAddress().getPort(), file.toFile()).isOk());
      assertTrue(multipart.get(), multipart.get().contains("Content-Type: image/png\r\n"));
    } finally {
      server.stop(0);
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void mimeLookupDoesNotDependOnTheDefaultLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      assertEquals("image/gif", ContentTypeUtils.getContentType(".GIF"));
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  public void authenticatedGetPreservesFailureStatusHeadersAndBody() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<String> authorization = new AtomicReference<>();
    server.createContext("/", exchange -> {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      int status = Integer.parseInt(exchange.getRequestURI().getPath().substring(1));
      byte[] body = "response".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("X-Test", "retained");
      exchange.sendResponseHeaders(status, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      for (int status : new int[] {200, 401, 500}) {
        ResponseVo response = HttpUtils.get("http://127.0.0.1:" + server.getAddress().getPort() + "/" + status, "test-token");
        assertEquals(status == 200, response.isOk());
        assertEquals(status, response.getCode());
        assertEquals("response", response.getBodyString());
        assertNotNull(response.getHeaders());
        assertEquals("retained", response.getHeaders().get("X-Test"));
        assertEquals("Bearer test-token", authorization.get());
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  public void explicitEmptyOrWhitespaceBodyIsNotChangedToAForm() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<String> contentType = new AtomicReference<>();
    AtomicReference<String> body = new AtomicReference<>();
    server.createContext("/", exchange -> {
      contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
      body.set(new String(IoUtils.toByteArray(exchange.getRequestBody()), StandardCharsets.UTF_8));
      exchange.sendResponseHeaders(204, -1);
      exchange.close();
    });
    server.start();
    try {
      for (String payload : new String[] {"", " \n\t", "{}"}) {
        try (Response response = HttpUtils.post("http://127.0.0.1:" + server.getAddress().getPort(), null, payload)) {
          assertEquals(204, response.code());
          assertEquals(payload, body.get());
          assertTrue(contentType.get().startsWith("application/json"));
        }
      }
    } finally {
      server.stop(0);
    }
  }
}
