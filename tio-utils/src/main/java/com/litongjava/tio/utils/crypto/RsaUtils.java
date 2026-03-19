package com.litongjava.tio.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RsaUtils {

  private static final String RSA_ALGORITHM = "RSA";

  /**
   * 生成私钥和公钥对
   *
   * @return 公钥和私钥的键值对
   * @throws Exception 如果生成密钥时发生错误
   */
  public static KeyPair generateKeyPair() {
    KeyPairGenerator keyPairGenerator = null;
    try {
      keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * 加密
   *
   * @param message   需要加密的数据
   * @param publicKey 公钥
   * @return 加密后的数据
   * @throws Exception 如果加密时发生错误
   */
  public static String encrypt(String message, PublicKey publicKey) {
    try {
      Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encryptedBytes);

    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * 解密
   *
   * @param encryptedMessage 需要解密的数据
   * @param privateKey       私钥
   * @return 解密后的数据
   * @throws Exception 如果解密时发生错误
   */
  public static String decrypt(String encryptedMessage, PrivateKey privateKey) {
    byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);

    Cipher cipher = null;
    try {
      cipher = Cipher.getInstance(RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (NoSuchPaddingException e) {
      throw new RuntimeException(e);
    }
    try {
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }

    byte[] decryptedBytes = null;
    try {
      decryptedBytes = cipher.doFinal(decodedBytes);
    } catch (IllegalBlockSizeException e) {
      throw new RuntimeException(e);
    } catch (BadPaddingException e) {
      throw new RuntimeException(e);
    }
    return new String(decryptedBytes, StandardCharsets.UTF_8);
  }

  /**
   * 获取公钥字符串
   *
   * @param keyPair 密钥对
   * @return 公钥字符串
   */
  public static String getPublicKeyString(KeyPair keyPair) {
    return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
  }

  /**
   * 获取私钥字符串
   *
   * @param keyPair 密钥对
   * @return 私钥字符串
   */
  public static String getPrivateKeyString(KeyPair keyPair) {
    return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
  }

  /**
   * 从字符串还原公钥
   *
   * @param publicKeyStr 公钥字符串
   * @return 公钥
   * @throws Exception 如果还原时发生错误
   */
  public static PublicKey getPublicKeyFromString(String publicKeyStr) {
    byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    try {
      return keyFactory.generatePublic(keySpec);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 从字符串还原私钥
   *
   * @param privateKeyStr 私钥字符串
   * @return 私钥
   * @throws Exception 如果还原时发生错误
   */
  public static PrivateKey getPrivateKeyFromString(String privateKeyStr) {
    byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
    } catch (NoSuchAlgorithmException e1) {
      throw new RuntimeException(e1);
    }
    try {
      return keyFactory.generatePrivate(keySpec);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }
}
