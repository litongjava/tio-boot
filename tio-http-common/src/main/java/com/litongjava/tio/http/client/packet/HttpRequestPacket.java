package com.litongjava.tio.http.client.packet;

import com.litongjava.aio.Packet;

@SuppressWarnings("serial")
public class HttpRequestPacket extends Packet {
  private final byte[] bytes;
  public HttpRequestPacket(byte[] bytes) { this.bytes = bytes; }
  public byte[] getBytes() { return bytes; }
}

