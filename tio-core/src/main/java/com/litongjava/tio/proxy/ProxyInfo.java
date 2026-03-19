package com.litongjava.tio.proxy;

public class ProxyInfo {
  private ProxyType proxyType;
  private String proxyHost;
  private Integer proxyPort;
  private String proxyUser;
  private String proxyPass;

  public ProxyInfo(String proxyHost, Integer proxyPort) {
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.proxyType = ProxyType.HTTP;
  }

  public ProxyInfo(String proxyHost, Integer proxyPort, ProxyType proxyType) {
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.proxyType = proxyType;
  }

  public ProxyInfo(ProxyType proxyType, String proxyHost, Integer proxyPort, String proxyUser, String proxyPass) {
    super();
    this.proxyType = proxyType;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.proxyUser = proxyUser;
    this.proxyPass = proxyPass;
  }

  public ProxyType getProxyType() {
    return proxyType;
  }

  public void setProxyType(ProxyType proxyType) {
    this.proxyType = proxyType;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public void setProxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public void setProxyPort(Integer proxyPort) {
    this.proxyPort = proxyPort;
  }

  public String getProxyUser() {
    return proxyUser;
  }

  public void setProxyUser(String proxyUser) {
    this.proxyUser = proxyUser;
  }

  public String getProxyPass() {
    return proxyPass;
  }

  public void setProxyPass(String proxyPass) {
    this.proxyPass = proxyPass;
  }

}
