package com.litongjava.tio.utils.email;

public class Wangyi126MailFactory implements IEMailFactory {

  @Override
  public EMail getMail() {
    return new Wangyi126Mail();
  }
}
