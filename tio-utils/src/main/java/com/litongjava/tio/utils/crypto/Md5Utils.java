package com.litongjava.tio.utils.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 utility.
 */
public class Md5Utils {

  private static final String DEFAULT_CHARSET = "UTF-8";

  /**
   * Convert a String into a byte array using the specified charset.
   *
   * @param content The input string to convert.
   * @param charset The name of the character encoding (e.g., "UTF-8").
   * @return The byte array representation of the input.
   * @throws RuntimeException if the charset is unsupported.
   */
  private static byte[] toBytes(String content, String charset) {
    if (charset == null || charset.isEmpty()) {
      return content.getBytes();
    }
    try {
      return content.getBytes(charset);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Error during MD5 processing: the specified charset is invalid: " + charset, e);
    }
  }

  /**
   * Convert a byte array to a lowercase hexadecimal string.
   *
   * @param bytes The input byte array.
   * @return A lowercase hex representation (each byte → two hex digits).
   */
  private static String bytesToHex(byte[] bytes) {
    char[] hexDigits = "0123456789abcdef".toCharArray();
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      int high = (b >> 4) & 0x0F;
      int low = b & 0x0F;
      sb.append(hexDigits[high]).append(hexDigits[low]);
    }
    return sb.toString();
  }

  /**
   * Compute the MD5 hash of a String (default charset UTF-8) and return it
   * as a lowercase hexadecimal string.
   *
   * @param input The input string.
   * @return Lowercase hex MD5 digest of the input.
   */
  public static String md5Hex(String input) {
    return md5Hex(input, DEFAULT_CHARSET);
  }

  /**
   * Compute the MD5 hash of a byte array and return it as a lowercase hex string.
   *
   * @param data The input byte array.
   * @return Lowercase hex MD5 digest.
   */
  public static String md5Hex(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(data);
      return bytesToHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // MD5 is guaranteed to exist in Java; this should never happen.
      throw new RuntimeException("Unable to get MD5 MessageDigest instance.", e);
    }
  }

  /**
   * Compute the MD5 hash of (text + key) using the specified charset,
   * returning a lowercase hexadecimal string.
   *
   * @param text          The base text to be signed.
   * @param key           The secret key to append.
   * @param inputCharset  The character encoding to use (e.g., "UTF-8").
   * @return MD5(text + key) as a lowercase hex string.
   */
  public static String sign(String text, String key, String inputCharset) {
    String combined = text + key;
    byte[] bytes = toBytes(combined, inputCharset);
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(bytes);
      return bytesToHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to get MD5 MessageDigest instance.", e);
    }
  }

  /**
   * Verify that the provided signature matches MD5(text + key).
   *
   * @param text          The original text.
   * @param signature     The expected MD5 signature (lowercase hex).
   * @param key           The secret key that was appended when signing.
   * @param inputCharset  The character encoding used during signing.
   * @return True if signature equals MD5(text + key), false otherwise.
   */
  public static boolean verify(String text, String signature, String key, String inputCharset) {
    // Since sign(text, key, charset) already does text+key, just call it directly:
    String computed = sign(text, key, inputCharset);
    return computed.equals(signature);
  }

  /**
   * Same as md5Hex(input), but allows specifying a charset.
   *
   * @param input   The input string.
   * @param charset The character encoding to use.
   * @return Lowercase hex MD5 digest.
   */
  public static String md5Hex(String input, String charset) {
    byte[] bytes = toBytes(input, charset);
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(bytes);
      return bytesToHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to get MD5 MessageDigest instance.", e);
    }
  }
}
