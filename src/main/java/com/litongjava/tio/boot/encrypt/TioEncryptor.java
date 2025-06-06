package com.litongjava.tio.boot.encrypt;

public interface TioEncryptor {

  public byte[] encrypt(byte[] soruces);

  public byte[] decrypt(byte[] soruces);
}
