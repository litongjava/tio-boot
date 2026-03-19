package com.litongjava.tio.boot.email;

public interface EmailSender {

  public boolean send(String to, String subject, String content);

  public boolean sendVerificationEmail(String to, String origin, String code);

  public boolean sendVerificationCodeEmail(String email, String origin, String code);
}
