package nexus.io.tio.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

public class HttpDownloadUtils {
  public static ByteArrayOutputStream download(String remoteUrl) {
    return download(remoteUrl, null);
  }

  public static ByteArrayOutputStream download(String remoteUrl, Map<String, String> headers) {
    HttpURLConnection connection = null;
    try {
      // Inherit the application's TLS trust and hostname verification policy.
      connection = (HttpURLConnection) new URL(remoteUrl).openConnection();
      if (headers != null) {
        for (Entry<String, String> entry : headers.entrySet()) {
          connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }
      connection.setDoInput(true);
      connection.setRequestMethod("GET");
      try (InputStream input = connection.getInputStream()) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = input.read(buffer)) != -1) {
          output.write(buffer, 0, length);
        }
        return output;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
