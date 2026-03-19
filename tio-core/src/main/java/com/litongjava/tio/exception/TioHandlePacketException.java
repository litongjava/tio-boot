package com.litongjava.tio.exception;

public class TioHandlePacketException extends RuntimeException {
  private static final long serialVersionUID = -291540616634229532L;

  public TioHandlePacketException() {
    super();
  }

  public TioHandlePacketException(String s) {
    super(s);
  }

  public TioHandlePacketException(Exception e) {
    super(e);
  }
}