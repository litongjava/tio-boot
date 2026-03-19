package com.litongjava.tio.utils.email;

public class GMailFactory implements IEMailFactory {

  @Override
  public EMail getMail() {
    return new GMail();
  }
}
