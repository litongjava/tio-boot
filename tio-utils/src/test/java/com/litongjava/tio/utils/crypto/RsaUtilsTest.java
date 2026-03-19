package com.litongjava.tio.utils.crypto;

import java.security.KeyPair;

import org.junit.Test;

public class RsaUtilsTest {

  @Test
  public void test() {
    // 生成密钥对
    KeyPair keyPair = RsaUtils.generateKeyPair();
    String publicKeyStr = RsaUtils.getPublicKeyString(keyPair);
    String privateKeyStr = RsaUtils.getPrivateKeyString(keyPair);

    System.out.println("公钥: " + publicKeyStr);
    System.out.println("私钥: " + privateKeyStr);

    // 加密和解密示例
    String originalMessage = "Hello, RSA!";
    System.out.println("原始消息: " + originalMessage);

    String encryptedMessage = RsaUtils.encrypt(originalMessage, RsaUtils.getPublicKeyFromString(publicKeyStr));
    System.out.println("加密消息: " + encryptedMessage);

    String decryptedMessage = RsaUtils.decrypt(encryptedMessage, RsaUtils.getPrivateKeyFromString(privateKeyStr));
    System.out.println("解密消息: " + decryptedMessage);
  }

}
