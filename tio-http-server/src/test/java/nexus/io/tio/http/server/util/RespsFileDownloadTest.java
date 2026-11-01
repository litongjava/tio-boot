package nexus.io.tio.http.server.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;

import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.common.handler.ITioHttpRequestHandler;
import nexus.io.tio.http.server.HttpServerAioHandler;
import nexus.io.tio.http.server.HttpServerAioListener;
import nexus.io.tio.server.ServerTioConfig;
import nexus.io.tio.server.TioServer;

public class RespsFileDownloadTest {

  private static final String PASSWORD = "test-password";

  @Test
  public void contentTypeFollowsExtension() {
    assertEquals(Resps.CONTENT_TYPE_EXCEL, Resps.contentTypeOf("a.XLSX"));
    assertEquals(Resps.CONTENT_TYPE_DOCX, Resps.contentTypeOf("a.docx"));
    assertEquals(Resps.CONTENT_TYPE_PPTX, Resps.contentTypeOf("a.pptx"));
    assertEquals("application/pdf", Resps.contentTypeOf("a.pdf"));
    assertEquals(Resps.CONTENT_TYPE_OCTET_STREAM, Resps.contentTypeOf("noext"));
  }

  @Test
  public void largeExportOverHttp() throws Exception {
    runDownload(null);
  }

  @Test
  public void largeExportOverHttps() throws Exception {
    Path dir = Files.createTempDirectory("resps-tls-");
    Path keyStore = dir.resolve("test.p12");
    try {
      String keytool = Paths.get(System.getProperty("java.home"), "bin", "keytool").toString();
      Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-storetype",
          "PKCS12", "-keystore", keyStore.toString(), "-storepass", PASSWORD, "-keypass", PASSWORD, "-dname",
          "CN=localhost", "-validity", "1", "-noprompt").redirectErrorStream(true)
              .redirectOutput(dir.resolve("keytool.log").toFile()).start();
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
      assertEquals(0, process.exitValue());
      runDownload(keyStore);
    } finally {
      Files.deleteIfExists(keyStore);
      Files.deleteIfExists(dir.resolve("keytool.log"));
      Files.deleteIfExists(dir);
    }
  }

  private void runDownload(Path keyStore) throws Exception {
    // Larger than one TLS record and one file chunk, so both send paths loop.
    byte[] content = new byte[3 * 1024 * 1024 + 123];
    new Random(7).nextBytes(content);
    final File file = Files.write(Files.createTempFile("export-", ".xlsx"), content).toFile();
    ExecutorService business = Executors.newFixedThreadPool(2);
    final HttpConfig http = new HttpConfig(0, false);
    ServerTioConfig config = new ServerTioConfig("resps-file-test");
    config.setWorkerThreads(1);
    config.setServerAioListener(new HttpServerAioListener());
    config.heartbeatTimeout = 0;
    config.setBizExecutor(business);
    if (keyStore != null) {
      try (InputStream key = Files.newInputStream(keyStore); InputStream trust = Files.newInputStream(keyStore)) {
        config.useSsl(key, trust, PASSWORD);
      }
    }
    config.setServerAioHandler(new HttpServerAioHandler(http, new ITioHttpRequestHandler() {
      public HttpResponse handler(HttpRequest request) throws Exception {
        HttpResponse response = new HttpResponse(request);
        if ("/missing".equals(request.getRequestLine().getPath())) {
          return Resps.excel(response, new File(file.getPath() + ".none"), "x.xlsx");
        }
        return Resps.excel(response, file, "报表 2026.xlsx");
      }

      public HttpResponse resp404(HttpRequest r, RequestLine l) {
        return null;
      }

      public HttpResponse resp500(HttpRequest r, RequestLine l, Throwable t) {
        return null;
      }

      public HttpConfig getHttpConfig(HttpRequest r) {
        return http;
      }

      public void clearStaticResCache() {
      }
    }));
    TioServer server = new TioServer(config);
    server.start("127.0.0.1", 0);
    try {
      int port = ((InetSocketAddress) server.getServerSocketChannel().getLocalAddress()).getPort();
      String scheme = keyStore == null ? "http" : "https";
      HttpURLConnection connection = open(scheme + "://localhost:" + port + "/export", keyStore);
      assertEquals(200, connection.getResponseCode());
      assertEquals(Resps.CONTENT_TYPE_EXCEL, connection.getContentType());
      assertEquals(String.valueOf(content.length), connection.getHeaderField("Content-Length"));
      assertEquals("no", connection.getHeaderField("X-Accel-Buffering"));
      assertEquals("attachment; filename=\"__ 2026.xlsx\"; filename*=UTF-8''%E6%8A%A5%E8%A1%A8%202026.xlsx",
          connection.getHeaderField("Content-Disposition"));
      assertArrayEquals(content, readAll(connection.getInputStream()));

      HttpURLConnection missing = open(scheme + "://localhost:" + port + "/missing", keyStore);
      assertEquals(404, missing.getResponseCode());
    } finally {
      server.stop();
      business.shutdownNow();
      Files.deleteIfExists(file.toPath());
    }
  }

  private static HttpURLConnection open(String url, Path keyStore) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    if (keyStore != null) {
      KeyStore trust = KeyStore.getInstance("PKCS12");
      try (InputStream input = Files.newInputStream(keyStore)) {
        trust.load(input, PASSWORD.toCharArray());
      }
      TrustManagerFactory managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      managers.init(trust);
      SSLContext tls = SSLContext.getInstance("TLS");
      tls.init(null, managers.getTrustManagers(), null);
      ((HttpsURLConnection) connection).setSSLSocketFactory(tls.getSocketFactory());
    }
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(10000);
    return connection;
  }

  private static byte[] readAll(InputStream input) throws Exception {
    try (InputStream in = input) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[64 * 1024];
      int n;
      while ((n = in.read(buffer)) != -1) {
        out.write(buffer, 0, n);
      }
      return out.toByteArray();
    }
  }
}
