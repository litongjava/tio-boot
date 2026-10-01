package nexus.io.tio.http.server;

import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import nexus.io.tio.http.common.*;
import nexus.io.tio.http.common.handler.ITioHttpRequestHandler;
import nexus.io.tio.server.*;

public class HttpKeepAliveLifecycleTest {
  static class Fixture implements AutoCloseable {
    final Path file = Files.createTempFile("keep-alive-body-", ".bin");
    final byte[] content = new byte[192 * 1024];
    final ExecutorService business = Executors.newFixedThreadPool(2);
    final TioServer server;
    Fixture() throws Exception {
      this(null);
    }
    Fixture(Path keyStore) throws Exception {
      new Random(17).nextBytes(content);
      Files.write(file, content);
      HttpConfig http = new HttpConfig(0, false);
      ServerTioConfig config = new ServerTioConfig("keep-alive-test");
      config.setWorkerThreads(1);
      config.heartbeatTimeout = 0;
      config.setBizExecutor(business);
      if (keyStore != null) {
        try (InputStream key = Files.newInputStream(keyStore); InputStream trust = Files.newInputStream(keyStore)) {
          config.useSsl(key, trust, "test-password");
        }
      }
      config.setServerAioHandler(new HttpServerAioHandler(http, new ITioHttpRequestHandler() {
        public HttpResponse handler(HttpRequest request) throws Exception {
          HttpResponse response = new HttpResponse(request);
          String path = request.getRequestLine().getPath();
          if (path.equals("/file")) response.setFileBody(file.toFile());
          else { if (path.equals("/first")) Thread.sleep(100); response.body(path); }
          return response;
        }
        public HttpResponse resp404(HttpRequest r, RequestLine l) { return null; }
        public HttpResponse resp500(HttpRequest r, RequestLine l, Throwable t) { return null; }
        public HttpConfig getHttpConfig(HttpRequest r) { return http; }
        public void clearStaticResCache() {}
      }));
      server = new TioServer(config);
      server.start("127.0.0.1", 0);
    }
    Socket connect() throws Exception {
      Socket socket = new Socket();
      socket.connect(server.getServerSocketChannel().getLocalAddress());
      socket.setSoTimeout(5000);
      return socket;
    }
    public void close() throws Exception {
      server.stop(); business.shutdownNow(); Files.deleteIfExists(file);
    }
  }
  static void send(Socket socket, String path, String version, String connection) throws Exception {
    String request = "GET " + path + " HTTP/" + version + "\r\nHost: localhost\r\n"
        + (connection == null ? "" : "Connection: " + connection + "\r\n") + "\r\n";
    socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
    socket.getOutputStream().flush();
  }
  static String line(InputStream input) throws Exception {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    for (int n; (n = input.read()) != '\n';) {
      if (n < 0) throw new EOFException("Unexpected end of HTTP headers");
      if (n != '\r') result.write(n);
      if (result.size() > 32768) throw new IOException("Oversized response header");
    }
    return result.toString("US-ASCII");
  }
  static byte[] response(Socket socket) throws Exception {
    InputStream input = socket.getInputStream();
    assertTrue(line(input).contains(" 200 "));
    int length = -1;
    for (String header; !(header = line(input)).isEmpty();) {
      if (header.toLowerCase(Locale.ROOT).startsWith("content-length:")) length = Integer.parseInt(header.substring(15).trim());
    }
    assertTrue("Response requires Content-Length", length >= 0);
    byte[] body = new byte[length]; new DataInputStream(input).readFully(body); return body;
  }
  @Test public void http11ReusesOneSocketAfterFileAndClosesOnlyWhenRequested() throws Exception {
    try (Fixture f = new Fixture(); Socket socket = f.connect()) {
      send(socket, "/file", "1.1", null); assertArrayEquals(f.content, response(socket));
      send(socket, "/second", "1.1", null); assertEquals("/second", new String(response(socket), StandardCharsets.UTF_8));
      send(socket, "/last", "1.1", "close"); assertEquals("/last", new String(response(socket), StandardCharsets.UTF_8));
      assertEquals(-1, socket.getInputStream().read());
    }
  }
  @Test public void pipelinedFileAndBusinessResponsesKeepTheirWireOrder() throws Exception {
    try (Fixture f = new Fixture(); Socket socket = f.connect()) {
      send(socket, "/first", "1.1", null); send(socket, "/file", "1.1", null); send(socket, "/last", "1.1", "close");
      assertEquals("/first", new String(response(socket), StandardCharsets.UTF_8));
      assertArrayEquals(f.content, response(socket));
      assertEquals("/last", new String(response(socket), StandardCharsets.UTF_8));
      assertEquals(-1, socket.getInputStream().read());
    }
  }
  @Test public void http10ExplicitKeepAliveReusesTheSocketAndDefaultCloses() throws Exception {
    try (Fixture f = new Fixture(); Socket socket = f.connect()) {
      send(socket, "/file", "1.0", "keep-alive"); assertArrayEquals(f.content, response(socket));
      send(socket, "/last", "1.0", null); assertEquals("/last", new String(response(socket), StandardCharsets.UTF_8));
      assertEquals(-1, socket.getInputStream().read());
    }
  }
  @Test public void httpsFileAndNextResponseReuseOneTlsConnection() throws Exception {
    Path directory = Files.createTempDirectory("keep-alive-tls-");
    Path keyStore = directory.resolve("test.p12");
    Path keyLog = directory.resolve("keytool.log");
    try {
      String keytool = Paths.get(System.getProperty("java.home"), "bin", "keytool").toString();
      Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "test", "-keyalg", "RSA",
          "-storetype", "PKCS12", "-keystore", keyStore.toString(), "-storepass", "test-password",
          "-keypass", "test-password", "-dname", "CN=localhost", "-validity", "1", "-noprompt")
          .redirectErrorStream(true).redirectOutput(keyLog.toFile()).start();
      if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Test certificate creation timed out"); }
      assertEquals("Test certificate creation failed", 0, process.exitValue());
      java.security.KeyStore trust = java.security.KeyStore.getInstance("PKCS12");
      try (InputStream input = Files.newInputStream(keyStore)) { trust.load(input, "test-password".toCharArray()); }
      javax.net.ssl.TrustManagerFactory managers = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
      managers.init(trust);
      javax.net.ssl.SSLContext tls = javax.net.ssl.SSLContext.getInstance("TLS");
      tls.init(null, managers.getTrustManagers(), null);
      try (Fixture f = new Fixture(keyStore); Socket socket = tls.getSocketFactory().createSocket()) {
        socket.connect(f.server.getServerSocketChannel().getLocalAddress()); socket.setSoTimeout(5000);
        send(socket, "/file", "1.1", null); assertArrayEquals(f.content, response(socket));
        send(socket, "/second", "1.1", "close"); assertEquals("/second", new String(response(socket), StandardCharsets.UTF_8));
        assertEquals(-1, socket.getInputStream().read());
      }
    } finally { Files.deleteIfExists(keyStore); Files.deleteIfExists(keyLog); Files.deleteIfExists(directory); }
  }
}
