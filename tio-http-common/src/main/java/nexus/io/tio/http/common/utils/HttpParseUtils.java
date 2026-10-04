package nexus.io.tio.http.common.utils;

/** HTTP header parameter parsing. @author tanyaowu */
public class HttpParseUtils {
  /** Returns a whole, case-insensitive parameter, decoding quoted pairs. */
  public static String getSubAttribute(String str, String name) {
    if (str == null || name == null || name.isEmpty()) {
      return null;
    }
    int position = 0;
    while (position < str.length()) {
      int start = position;
      boolean quoted = false;
      boolean escaped = false;
      while (position < str.length()) {
        char c = str.charAt(position);
        if (escaped) {
          escaped = false;
        } else if (quoted && c == '\\') {
          escaped = true;
        } else if (c == '"') {
          quoted = !quoted;
        } else if (!quoted && c == ';') {
          break;
        }
        position++;
      }
      if (quoted || escaped) {
        return null;
      }
      String part = str.substring(start, position).trim();
      position++;
      int equals = part.indexOf('=');
      if (equals < 0 || !part.substring(0, equals).trim().equalsIgnoreCase(name)) {
        continue;
      }
      String value = part.substring(equals + 1).trim();
      if (!value.startsWith("\"")) {
        return value.indexOf('"') < 0 ? value : null;
      }
      StringBuilder decoded = new StringBuilder();
      for (int i = 1; i < value.length(); i++) {
        char c = value.charAt(i);
        if (c == '\\') {
          i++;
          if (i == value.length()) {
            return null;
          }
          decoded.append(value.charAt(i));
        } else if (c == '"') {
          return i == value.length() - 1 ? decoded.toString() : null;
        } else {
          decoded.append(c);
        }
      }
      return null;
    }
    return null;
  }

  private HttpParseUtils() {
  }
}
