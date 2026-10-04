package nexus.io.tio.utils.http;

import static org.junit.Assert.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;

public class HttpDownloadBoundaryTest {
  @Test
  public void initializationDoesNotChangeGlobalTlsPolicy() throws Exception {
    SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();
    HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
    URL classes = HttpDownloadUtils.class.getProtectionDomain().getCodeSource().getLocation();
    try (URLClassLoader loader = new URLClassLoader(new URL[] {classes}, null)) {
      Class.forName("nexus.io.tio.utils.http.HttpDownloadUtils", true, loader);
      assertSame(factory, HttpsURLConnection.getDefaultSSLSocketFactory());
      assertSame(verifier, HttpsURLConnection.getDefaultHostnameVerifier());
    } finally {
      HttpsURLConnection.setDefaultSSLSocketFactory(factory);
      HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    }
  }

  @Test
  public void downloadsBytesWithHeadersAndReportsHttpErrors() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<String> method = new AtomicReference<>();
    AtomicReference<String> header = new AtomicReference<>();
    byte[] bytes = "下载测试".getBytes(StandardCharsets.UTF_8);
    server.createContext("/file", exchange -> {
      method.set(exchange.getRequestMethod());
      header.set(exchange.getRequestHeaders().getFirst("X-Test"));
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.createContext("/missing", exchange -> {
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
    });
    server.start();
    try {
      String base = "http://127.0.0.1:" + server.getAddress().getPort();
      assertArrayEquals(bytes, HttpDownloadUtils.download(base + "/file", Collections.singletonMap("X-Test", "yes")).toByteArray());
      assertEquals("GET", method.get());
      assertEquals("yes", header.get());
      try {
        HttpDownloadUtils.download(base + "/missing");
        fail("HTTP errors must not return a successful download");
      } catch (RuntimeException expected) {
        assertTrue(expected.getCause() instanceof java.io.IOException);
      }
    } finally {
      server.stop(0);
    }
  }
}
