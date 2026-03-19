package com.litongjava.tio.utils.hex;

public class HexUtils {

  private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

  /**
   * 将十六进制字符串解码为字节数组。
   * 
   * @param hexStr 仅包含 0-9, a-f, A-F 字符，且长度必须为偶数。
   * @return 对应的 byte[]（每两个十六进制字符对应一个字节）
   * @throws IllegalArgumentException 如果输入长度不是偶数或包含非法字符
   */
  public static byte[] decodeHex(String hexStr) {
    if (hexStr == null) {
      throw new IllegalArgumentException("Input hex string must not be null");
    }
    int len = hexStr.length();
    if ((len & 1) != 0) {
      throw new IllegalArgumentException("Hex string length must be even: " + hexStr);
    }
    byte[] result = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      int high = charToHexDigit(hexStr.charAt(i));
      int low = charToHexDigit(hexStr.charAt(i + 1));
      result[i / 2] = (byte) ((high << 4) | low);
    }
    return result;
  }

  /**
   * 将字节数组编码为十六进制字符串（小写字母）。
   * 
   * @param bytes 任意字节数组
   * @return 对应的十六进制字符串表示（长度为 bytes.length * 2）
   */
  public static String encode(byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("Input byte array must not be null");
    }
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * 将单个十六进制字符转换为 0–15 之间的整数。
   * 
   * @param c 十六进制字符：0–9、a–f、A–F
   * @return 对应的整数值
   * @throws IllegalArgumentException 如果字符不在有效范围内
   */
  private static int charToHexDigit(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'A' && c <= 'F') {
      return 10 + (c - 'A');
    } else if (c >= 'a' && c <= 'f') {
      return 10 + (c - 'a');
    } else {
      throw new IllegalArgumentException("Invalid hex character: " + c);
    }
  }
}
