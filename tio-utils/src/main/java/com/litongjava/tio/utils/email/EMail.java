package com.litongjava.tio.utils.email;

public interface EMail {

  public void send(String to, String subject, String content, boolean isDebug);

}
