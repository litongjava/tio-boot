package com.litongjava.tio.core;

import java.util.Objects;

/**
 * 连接关闭码
 * 
 * @author tanyaowu
 */
public enum ChannelCloseCode {
  /**
   * 没有提供原因码
   */
  NO_CODE((byte) 1),
  /**
   * 读异常
   */
  READ_ERROR((byte) 2),
  /**
   * 写异常
   */
  WRITER_ERROR((byte) 3),
  /**
   * 解码异常
   */
  DECODE_ERROR((byte) 4),
  /**
   * 通道未打开
   */
  CHANNEL_NOT_OPEN((byte) 5),
  /**
   * 读到的数据长度是0
   */
  READ_COUNT_IS_ZERO((byte) 6),
  /**
   * 对方关闭了连接
   */
  CLOSED_BY_PEER((byte) 7),
  /**
   * 读到的数据长度小于-1
   */
  READ_COUNT_IS_NEGATIVE((byte) 8),
  /**
   * 写数据长度小于0
   */
  WRITE_COUNT_IS_NEGATIVE((byte) 9),

  SERVER_CONNECTION((byte) 9),
  /**
   * 心跳超时
   */
  HEARTBEAT_TIMEOUT((byte) 10),
  /**
   * 连接失败
   */
  CLIENT_CONNECTION_FAIL((byte) 80),

  /**
   * SSL握手时发生异常
   */
  SSL_ERROR_ON_HANDSHAKE((byte) 50),
  /**
   * SSL session关闭了
   */
  SSL_SESSION_CLOSED((byte) 51),
  /**
   * SSL加密时发生异常
   */
  SSL_ENCRYPTION_ERROR((byte) 52),
  /**
   * SSL解密时发生异常
   */
  SSL_DECRYPT_ERROR((byte) 53),

  /**
   * 供用户使用
   */
  USER_CODE_0((byte) 100),
  /**
   * 供用户使用
   */
  USER_CODE_1((byte) 101),
  /**
   * 供用户使用
   */
  USER_CODE_2((byte) 102),
  /**
   * 供用户使用
   */
  USER_CODE_3((byte) 103),
  /**
   * 供用户使用
   */
  USER_CODE_4((byte) 104),
  /**
   * 供用户使用
   */
  USER_CODE_5((byte) 105),
  /**
   * 供用户使用
   */
  USER_CODE_6((byte) 106),
  /**
   * 供用户使用
   */
  USER_CODE_7((byte) 107),
  /**
   * 供用户使用
   */
  USER_CODE_8((byte) 108),
  /**
   * 供用户使用
   */
  USER_CODE_9((byte) 109),
  /**
   * 供用户使用
   */
  USER_CODE_10((byte) 110),
  /**
   * 初始值
   */
  INIT_STATUS((byte) 199),
  /**
   * 其它异常
   */
  OTHER_ERROR((byte) 200),
  /***
   * 超出最大包长度
   */
  PACKET_TOO_LARGE((byte) 201),
  /**
   * 错误
   */
  CLOSE_BY_ERROR((byte) 202),

  IP_IN_BLACK_LIST((byte) 203);

  public static ChannelCloseCode from(Byte value) {
    ChannelCloseCode[] values = ChannelCloseCode.values();
    for (ChannelCloseCode v : values) {
      if (Objects.equals(v.value, value)) {
        return v;
      }
    }
    return null;
  }

  Byte value;

  private ChannelCloseCode(Byte value) {
    this.value = value;
  }

  public Byte getValue() {
    return value;
  }

  public void setValue(Byte value) {
    this.value = value;
  }
}