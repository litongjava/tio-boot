package com.litongjava.tio.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpDownloadUtils {

  static {
    // 设置一个信任所有证书的 TrustManager
    X509TrustManager x509TrustManager = new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    };

    TrustManager[] trustAllCerts = new TrustManager[] { x509TrustManager };

    // 设置全局的 SSLContext 以信任所有的证书
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ByteArrayOutputStream download(String remoteUrl) {
    return download(remoteUrl, null);
  }

  public static ByteArrayOutputStream download(String remoteUrl, Map<String, String> headers) {
    URL httpURL;
    try {
      httpURL = new URL(remoteUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    HttpURLConnection httpConnection;
    try {
      httpConnection = (HttpURLConnection) httpURL.openConnection();
      if (httpConnection instanceof HttpsURLConnection) {
        ((HttpsURLConnection) httpConnection).setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
        ((HttpsURLConnection) httpConnection).setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // 设置 HTTP 头
    if (headers != null && headers.size() > 0) {
      Set<Entry<String, String>> entrySet = headers.entrySet();
      for (Entry<String, String> entry : entrySet) {
        httpConnection.setRequestProperty(entry.getKey(), entry.getValue());
      }
    }

    httpConnection.setDoInput(true);
    httpConnection.setDoOutput(true);

    try (InputStream inputStream = httpConnection.getInputStream(); ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

      int length;
      byte[] buffer = new byte[1024 * 1024 * 2]; // 一次读取2M

      while ((length = inputStream.read(buffer)) != -1) {
        byteArrayOutputStream.write(buffer, 0, length); // 将文件保存到内存流
      }

      return byteArrayOutputStream;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
