package com.litongjava.tio.http.client.packet;

import com.litongjava.aio.Packet;

@SuppressWarnings("serial")
public class HttpResponsePacket extends Packet {
  public int statusCode;
  public String statusLine;
  public java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
  public byte[] body;
}
