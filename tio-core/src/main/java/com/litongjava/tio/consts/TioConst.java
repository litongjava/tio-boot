package com.litongjava.tio.consts;

public interface TioConst {

  /**
   * 默认规定连接到本服务器的客户端统一用utf-8
   */
  String UTF_8 = "utf-8";

  // 建议统一 key，避免魔法字符串散落
  String ATTR_TLS_PEER_HOST = "tio.tls.peerHost";
  String ATTR_TLS_PEER_PORT = "tio.tls.peerPort";
  String ATTR_SSL_HANDSHAKE_LATCH = "TIO_SSL_HANDSHAKE_LATCH";
}
