package com.litongjava.tio.utils.digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.litongjava.tio.utils.base64.Base64Utils;

/**
 * 安全的密码哈希工具类，基于 PBKDF2WithHmacSHA256 算法。
 * <p>
 * 该实现不依赖任何第三方库，仅使用 Java 8 原生 API。
 * 它实现了加盐和可配置的迭代次数，以抵御暴力破解。
 * </p>
 * <p>
 * 存储格式: iterations$salt$hash
 * - salt 和 hash 都使用 Base64 编码。
 * </p>
 * 这个工具类是线程安全的。
 */
public final class Sha256Utils {

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

  /**
   * 迭代次数。这个值越高，哈希计算越慢，密码越安全。
   * 2023年 OWASP 推荐值为 600,000 (对于 HMAC-SHA256)。
   * 可以根据服务器性能进行调整。
   */
  public static final int ITERATIONS = 600000;

  /**
   * 哈希值的期望长度（单位：位）。256位对于SHA256是标准长度。
   */
  private static final int KEY_LENGTH = 256;

  /**
   * 盐的长度（单位：字节）。16字节（128位）是推荐的最小值。
   */
  private static final int SALT_LENGTH = 16;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * 私有构造函数，防止该工具类被实例化。
   */
  private Sha256Utils() {
    // 防止实例化
  }

  /**
   * 对明文密码进行哈希处理。
   *
   * @param plainTextPassword 需要哈希的明文密码。
   * @return 一个包含迭代次数、盐和哈希值的组合字符串，格式为 "iterations$salt$hash"。
   * @throws RuntimeException 如果加密算法不可用（在标准Java环境中极不可能发生）。
   */
  public static String hashPassword(String plainTextPassword) {
    if (plainTextPassword == null || plainTextPassword.isEmpty()) {
      throw new IllegalArgumentException("Password cannot be null or empty.");
    }

    // 1. 生成随机盐
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);

    // 2. 哈希密码
    byte[] hash = pbkdf2(plainTextPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

    // 3. 将盐和哈希值编码为 Base64 字符串
    String saltBase64 = Base64Utils.encodeToString(salt);
    String hashBase64 = Base64Utils.encodeToString(hash);

    // 4. 组合成最终的存储格式
    return ITERATIONS + "$" + saltBase64 + "$" + hashBase64;
  }

  /**
   * 验证明文密码是否与一个已哈希的密码匹配。
   *
   * @param plainTextPassword 用户输入的明文密码。
   * @param hashedPassword    从数据库中取出的、格式为 "iterations$salt$hash" 的哈希字符串。
   * @return 如果密码匹配，则返回 true；否则返回 false。
   */
  public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
    if (plainTextPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
      return false;
    }

    // 1. 解析存储的哈希字符串
    String[] parts = hashedPassword.split("\\$");
    if (parts.length != 3) {
      // 格式不正确
      return false;
    }

    try {
      int iterations = Integer.parseInt(parts[0]);
      byte[] salt = Base64.getDecoder().decode(parts[1]);
      byte[] storedHash = Base64.getDecoder().decode(parts[2]);

      // 2. 使用相同的参数（盐和迭代次数）对用户输入的密码进行哈希
      byte[] inputHash = pbkdf2(plainTextPassword.toCharArray(), salt, iterations, KEY_LENGTH);

      // 3. 比较两个哈希值 (使用时间恒定的比较方法防止时序攻击)
      return slowEquals(storedHash, inputHash);

    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * PBKDF2 核心哈希函数。
   */
  private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
      SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
      return skf.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      // 在标准的 Java 环境中，PBKDF2WithHmacSHA256 总是可用的。
      // 如果发生此异常，说明环境有问题。
      throw new RuntimeException("Error while hashing password: " + e.getMessage(), e);
    }
  }

  /**
   * 使用时间恒定的方法比较两个字节数组，以防止时序攻击。
   * 无论在哪个位置发现不匹配，此方法都会完整地比较完所有字节，
   * 从而使得攻击者无法通过测量响应时间来推断匹配的进度。
   *
   * @param a 第一个字节数组。
   * @param b 第二个字节数组。
   * @return 如果两个数组内容完全相同，则返回 true。
   */
  private static boolean slowEquals(byte[] a, byte[] b) {
    int diff = a.length ^ b.length;
    for (int i = 0; i < a.length && i < b.length; i++) {
      diff |= a[i] ^ b[i];
    }
    return diff == 0;
  }

  public static String digestToHex(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}