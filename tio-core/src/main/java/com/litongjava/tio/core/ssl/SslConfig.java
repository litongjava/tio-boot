package com.litongjava.tio.core.ssl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.litongjava.tio.utils.hutool.ResourceUtil;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * @author Tong Li
 */
public class SslConfig {

  private InputStream keyStoreInputStream = null;
  private InputStream trustStoreInputStream = null;
  private String passwd = null;

  private KeyManagerFactory keyManagerFactory;
  private TrustManagerFactory trustManagerFactory;

  private boolean isClient;

  private SslConfig(InputStream keyStoreInputStream, InputStream trustStoreInputStream, String passwd, boolean isClient)
      throws Exception {
    this.keyStoreInputStream = keyStoreInputStream;
    this.trustStoreInputStream = trustStoreInputStream;
    this.passwd = passwd;
    this.isClient = isClient;
    this.init();
  }

  public static SslConfig forServer(String keyStoreFile, String trustStoreFile, String passwd) throws Exception {
    InputStream keyStoreInputStream;
    InputStream trustStoreInputStream;

    if (StrUtil.startWithIgnoreCase(keyStoreFile, "classpath:")) {
      keyStoreInputStream = ResourceUtil.getResourceAsStream(keyStoreFile);
    } else {
      keyStoreInputStream = new FileInputStream(keyStoreFile);
    }

    if (StrUtil.startWithIgnoreCase(trustStoreFile, "classpath:")) {
      trustStoreInputStream = ResourceUtil.getResourceAsStream(trustStoreFile);
    } else {
      trustStoreInputStream = new FileInputStream(trustStoreFile);
    }

    return forServer(keyStoreInputStream, trustStoreInputStream, passwd);
  }

  public static SslConfig forServer(InputStream keyStoreInputStream, InputStream trustStoreInputStream, String passwd)
      throws Exception {
    return new SslConfig(keyStoreInputStream, trustStoreInputStream, passwd, false);
  }

  /**
   * 客户端默认使用“系统默认信任库”（JDK cacerts）
   */
  public static SslConfig forClient() throws Exception {
    // 注意：isClient=true，且两个 InputStream 都为 null，表示用默认信任库
    return new SslConfig(null, null, null, true);
  }

  /**
   * 客户端：指定自定义 truststore
   */
  public static SslConfig forClient(InputStream trustStoreInputStream, String passwd) throws Exception {
    return new SslConfig(null, trustStoreInputStream, passwd, true);
  }

  public void init() throws Exception {
    char[] passChars = null;
    if (passwd != null) {
      passChars = passwd.toCharArray();
    }

    KeyStore keyStore = null;
    if (keyStoreInputStream != null) {
      keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(keyStoreInputStream, passChars);
    }

    keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, passChars);

    trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    if (trustStoreInputStream != null) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(trustStoreInputStream, passChars);
      trustManagerFactory.init(trustStore);
    } else {
      trustManagerFactory.init((KeyStore) null);
    }
  }

  public KeyManagerFactory getKeyManagerFactory() {
    return keyManagerFactory;
  }

  public TrustManagerFactory getTrustManagerFactory() {
    return trustManagerFactory;
  }
}
