package com.litongjava.tio.core;

import java.util.Objects;

import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * 
 * @author tanyaowu 2017年10月19日 上午9:40:07
 */
public class Node implements Comparable<Node> {
  private String host;
  private int port;
  private Byte ssl = 1;

  public Node(String ip, int port) {
    super();
    if (StrUtil.isBlank(ip)) {
      ip = "0.0.0.0";
    }

    this.setHost(ip);
    this.setPort(port);
  }

  @Override
  public int compareTo(Node other) {
    if (other == null) {
      return -1;
    }
    // RemoteNode other = (RemoteNode) obj;

    if (Objects.equals(host, other.getHost()) && Objects.equals(port, other.getPort())) {
      return 0;
    } else {
      return this.toString().compareTo(other.toString());
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    Node other = (Node) obj;
    return host.equals(other.getHost()) && port == other.getPort();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public int hashCode() {
    return (host + ":" + port).hashCode();
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(host).append(":").append(port);
    return builder.toString();
  }

  /**
   * @return the ssl
   */
  public Byte getSsl() {
    return ssl;
  }

  /**
   * @param ssl the ssl to set
   */
  public void setSsl(Byte ssl) {
    this.ssl = ssl;
  }

  public String setIp(String ip) {
    return this.host = ip;
  }

  public String getIp() {
    return this.host;
  }

}
