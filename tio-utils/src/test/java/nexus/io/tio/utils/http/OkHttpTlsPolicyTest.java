package nexus.io.tio.utils.http;

import static org.junit.Assert.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.testng.annotations.Test;

public class OkHttpTlsPolicyTest {
  @Test
  public void pooledClientsAndTrustHelperRejectUntrustedCertificate() throws Exception {
    Path directory = Files.createTempDirectory("client-tls-policy-");
    Path store = directory.resolve("test.p12");
    Path log = directory.resolve("keytool.log");
    HttpsServer server = null;
    try {
      String keytool = Paths.get(System.getProperty("java.home"), "bin", "keytool").toString();
      Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "test", "-keyalg", "RSA",
          "-storetype", "PKCS12", "-keystore", store.toString(), "-storepass", "test-password",
          "-keypass", "test-password", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost",
          "-validity", "1", "-noprompt").redirectErrorStream(true).redirectOutput(log.toFile()).start();
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        fail("Test certificate generation timed out");
      }
      assertEquals(0, process.exitValue());
      KeyStore keys = KeyStore.getInstance("PKCS12");
      try (InputStream input = Files.newInputStream(store)) {
        keys.load(input, "test-password".toCharArray());
      }
      KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      managers.init(keys, "test-password".toCharArray());
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(managers.getKeyManagers(), null, null);
      server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
      server.setHttpsConfigurator(new HttpsConfigurator(context));
      server.createContext("/", exchange -> {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
      });
      server.start();
      Request request = new Request.Builder().url("https://localhost:" + server.getAddress().getPort()).build();
      OkHttpClient[] clients = {OkHttpClientPool.getHttpClient(), OkHttpClientPool.get60HttpClient(),
          OkHttpClientPool.get120HttpClient(), OkHttpClientPool.get300HttpClient(),
          OkHttpClientPool.get600HttpClient(), OkHttpClientPool.get1000HttpClient(),
          OkHttpClientPool.get1200HttpClient(), OkHttpClientPool.get3600HttpClient()};
      for (OkHttpClient client : clients) {
        try (Response response = client.newCall(request).execute()) {
          fail("An untrusted certificate must not produce an HTTP response: " + response.code());
        } catch (SSLException expected) {
          // A locally generated certificate is not in the JVM trust store.
        }
      }
      try {
        OkHttpClientPool.x509TrustManager().checkServerTrusted(
            new X509Certificate[] {(X509Certificate) keys.getCertificate("test")}, "RSA");
        fail("The public trust manager must also validate the certificate chain");
      } catch (CertificateException expected) {
        // Same trust contract as the pooled clients.
      }
      assertNotNull(OkHttpClientPool.sslSocketFactory());
    } finally {
      if (server != null) {
        server.stop(0);
      }
      Files.deleteIfExists(store);
      Files.deleteIfExists(log);
      Files.deleteIfExists(directory);
    }
  }
}
