package com.litongjava.tio.utils.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class UrlUtils {

  // Unreserved characters as defined in RFC 3986
  private static final String UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
  // Create a BitSet to mark the byte values of the unreserved characters
  private static final BitSet UNRESERVED_CHARACTERS = new BitSet(256);
  static {
    for (char c : UNRESERVED.toCharArray()) {
      UNRESERVED_CHARACTERS.set(c);
    }
  }

  /**
   * Encodes the given string according to RFC 3986.
   * @param value the string to encode
   * @return the encoded string
   */
  public static String encode(String value) {
    if (value == null) {
      return null;
    }
    StringBuilder encoded = new StringBuilder();
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      // Convert the byte to an unsigned integer value
      int c = b & 0xFF;
      if (UNRESERVED_CHARACTERS.get(c)) {
        // Append unreserved characters directly
        encoded.append((char) c);
      } else {
        // Encode other characters in the %HH format
        encoded.append(String.format("%%%02X", c));
      }
    }
    return encoded.toString();
  }

  /**
   * Decodes a string that has been encoded according to RFC 3986.
   * @param value the encoded string
   * @return the decoded string
   */
  public static String decode(String value) {
    if (value == null) {
      return null;
    }
    // Use a byte array to handle the decoded bytes
    int length = value.length();
    byte[] buffer = new byte[length];
    int pos = 0;
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      if (c == '%') {
        if (i + 2 < length) {
          // Treat the next two characters as a hexadecimal value
          String hex = value.substring(i + 1, i + 3);
          buffer[pos++] = (byte) Integer.parseInt(hex, 16);
          i += 2;
        } else {
          throw new IllegalArgumentException("Invalid percent-encoding in: " + value);
        }
      } else {
        // Directly store the byte of a normal character
        buffer[pos++] = (byte) c;
      }
    }
    return new String(buffer, 0, pos, StandardCharsets.UTF_8);
  }

  /**
   * Encodes the path component of a URL while preserving the fixed parts of the URL (e.g., scheme, host, port).
   * <p>
   * For example:<br>
   * Input:  https://www.kapiolani.hawaii.edu/wp-content/uploads/2018-Kapi‘olani-Community-College-Technology-Plan.pdf<br>
   * Output: https://www.kapiolani.hawaii.edu/wp-content/uploads/2018-Kapi%E2%80%98olani-Community-College-Technology-Plan.pdf
   * </p>
   * @param url the original URL
   * @return the encoded URL
   */
  public static String encodeUrl(String url) {
    if (url == null) {
      return null;
    }
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      // Get the raw authority (includes user info, host, port) without further encoding
      String authority = uri.getRawAuthority();
      String path = uri.getPath();
      // Encode the path while preserving the '/' delimiter
      String encodedPath = encodePath(path);

      // Handle query and fragment (assumed to be already encoded; similar processing can be done if needed)
      String query = uri.getRawQuery();
      String fragment = uri.getRawFragment();

      StringBuilder result = new StringBuilder();
      if (scheme != null) {
        result.append(scheme).append(":");
      }
      if (authority != null) {
        result.append("//").append(authority);
      }
      result.append(encodedPath);
      if (query != null) {
        result.append("?").append(query);
      }
      if (fragment != null) {
        result.append("#").append(fragment);
      }
      return result.toString();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URL: " + url, e);
    }
  }

  /**
   * Encodes the path component of a URL, preserving unreserved characters and the '/' delimiter.
   * @param path the URL path
   * @return the encoded path
   */
  private static String encodePath(String path) {
    if (path == null) {
      return null;
    }
    StringBuilder encoded = new StringBuilder();
    // Define the allowed characters in the path (unreserved characters + '/')
    BitSet allowed = new BitSet(256);
    for (char c : UNRESERVED.toCharArray()) {
      allowed.set(c);
    }
    allowed.set('/');

    byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
    for (byte b : bytes) {
      int c = b & 0xFF;
      if (allowed.get(c)) {
        encoded.append((char) c);
      } else {
        encoded.append(String.format("%%%02X", c));
      }
    }
    return encoded.toString();
  }
}
