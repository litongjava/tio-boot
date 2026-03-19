package com.litongjava.tio.utils.http;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.litongjava.tio.utils.environment.EnvUtils;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;

public enum OkHttpClientPool {
  INSTANCE;

  private static final ConnectionPool SHARED_CONNECTION_POOL = new ConnectionPool(200, 5, TimeUnit.MINUTES);

  private static final OkHttpClient CLIENT_30S;
  private static final OkHttpClient CLIENT_60S;
  private static final OkHttpClient CLIENT_120S;
  private static final OkHttpClient CLIENT_300S;
  private static final OkHttpClient CLIENT_600S;
  private static final OkHttpClient CLIENT_1000S;
  private static final OkHttpClient CLIENT_1200S;
  private static final OkHttpClient CLIENT_3600S;

  static {
    // 从系统属性获取代理设置
    String proxyHost = EnvUtils.get("http.proxyHost");
    String proxyPort = EnvUtils.get("http.proxyPort");

    Proxy proxy = null;
    if (proxyHost != null && proxyPort != null) {
      proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
    }

    // 30秒超时客户端
    OkHttpClient.Builder builder30 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        //
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder30.proxy(proxy);
    }
    CLIENT_30S = builder30.build();

    // 60秒超时客户端
    OkHttpClient.Builder builder60 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        //
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(60L, TimeUnit.SECONDS).readTimeout(60L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder60.proxy(proxy);
    }
    CLIENT_60S = builder60.build();

    // 120秒超时客户端
    OkHttpClient.Builder builder120 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(120L, TimeUnit.SECONDS).readTimeout(120L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder120.proxy(proxy);
    }
    CLIENT_120S = builder120.build();

    // 300秒超时客户端
    OkHttpClient.Builder builder300 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(300L, TimeUnit.SECONDS).readTimeout(300L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder300.proxy(proxy);
    }
    CLIENT_300S = builder300.build();

    // 600秒超时客户端
    OkHttpClient.Builder builder600 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        //
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(600L, TimeUnit.SECONDS).readTimeout(600L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder600.proxy(proxy);
    }
    CLIENT_600S = builder600.build();

    // 1000秒超时客户端
    OkHttpClient.Builder builder1000 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(1000L, TimeUnit.SECONDS).readTimeout(1000L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder1000.proxy(proxy);
    }
    CLIENT_1000S = builder1000.build();

    // 1200秒超时客户端
    OkHttpClient.Builder builder1200 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        //
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(1200L, TimeUnit.SECONDS).readTimeout(1200L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder1200.proxy(proxy);
    }
    CLIENT_1200S = builder1200.build();

    // 3600秒超时客户端
    OkHttpClient.Builder builder3600 = new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE)
        //
        .connectionPool(SHARED_CONNECTION_POOL).sslSocketFactory(sslSocketFactory(), x509TrustManager())
        //
        .connectTimeout(3600L, TimeUnit.SECONDS).readTimeout(3600L, TimeUnit.SECONDS);
    if (proxy != null) {
      builder3600.proxy(proxy);
    }
    CLIENT_3600S = builder3600.build();
  }

  public static OkHttpClient getHttpClient() {
    return CLIENT_30S;
  }

  public static OkHttpClient get60HttpClient() {
    return CLIENT_60S;
  }

  public static OkHttpClient get120HttpClient() {
    return CLIENT_120S;
  }

  public static OkHttpClient get300HttpClient() {
    return CLIENT_300S;
  }

  public static OkHttpClient get600HttpClient() {
    return CLIENT_600S;
  }

  public static OkHttpClient get1000HttpClient() {
    return CLIENT_1000S;
  }

  public static OkHttpClient get1200HttpClient() {
    return CLIENT_1200S;
  }

  public static OkHttpClient get3600HttpClient() {
    return CLIENT_3600S;
  }

  public static X509TrustManager x509TrustManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
  }

  public static SSLSocketFactory sslSocketFactory() {
    try {
      // 信任任何链接
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] { x509TrustManager() }, new SecureRandom());
      return sslContext.getSocketFactory();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
    return null;
  }
}