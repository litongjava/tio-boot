package nexus.io.tio.utils.url;

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
    int length = value.length();
    byte[] buffer = new byte[length];
    StringBuilder decoded = new StringBuilder(length);
    int index = 0;
    while (index < length) {
      if (value.charAt(index) == '%') {
        int count = 0;
        while (index < length && value.charAt(index) == '%') {
          if (index + 2 >= length) {
            throw new IllegalArgumentException("Incomplete percent-encoding");
          }
          int high = hexDigit(value.charAt(index + 1));
          int low = hexDigit(value.charAt(index + 2));
          if (high < 0 || low < 0) {
            throw new IllegalArgumentException("Invalid percent-encoding");
          }
          buffer[count++] = (byte) ((high << 4) | low);
          index += 3;
        }
        decoded.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
      } else {
        decoded.append(value.charAt(index++));
      }
    }
    return decoded.toString();
  }

  private static int hexDigit(char value) {
    if (value >= '0' && value <= '9') {
      return value - '0';
    }
    if (value >= 'a' && value <= 'f') {
      return value - 'a' + 10;
    }
    if (value >= 'A' && value <= 'F') {
      return value - 'A' + 10;
    }
    return -1;
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
      if (uri.isOpaque()) {
        return uri.toASCIIString();
      }
      String path = uri.getRawPath();
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
   * Encodes a raw URI path, preserving validated escapes and path delimiters.
   * @param path the URL path
   * @return the encoded path
   */
  private static String encodePath(String path) {
    if (path == null) {
      return "";
    }
    StringBuilder encoded = new StringBuilder();
    // Define the allowed characters in the path (unreserved characters + '/')
    BitSet allowed = new BitSet(256);
    for (char c : UNRESERVED.toCharArray()) {
      allowed.set(c);
    }
    allowed.set('/');
    // URI parsing already validated percent escapes; retain them and path delimiters.
    for (char c : "%!$&'()*+,;=:@".toCharArray()) {
      allowed.set(c);
    }

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
