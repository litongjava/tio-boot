package com.litongjava.tio.http.client.packet;

public class ChunkParseResult {
  public byte[] body;
  public int consumedBytes; // 从 bodyStart 开始 consumed 了多少字节（包括 chunk size 行、数据、CRLF、终止块等）
}