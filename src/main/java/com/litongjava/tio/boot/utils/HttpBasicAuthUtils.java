package com.litongjava.tio.boot.utils;

import java.util.Base64;

public class HttpBasicAuthUtils {

  public static boolean authenticate(String authorization, String username, String password) {
    if (authorization != null && authorization.startsWith("Basic ")) {
      String base64Credentials = authorization.substring("Basic ".length()).trim();
      byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
      String credentials = new String(decodedBytes);

      final String[] values = credentials.split(":", 2);
      String inptuUsername = values[0];
      String inputPassword = values[1];

      return username.equals(inptuUsername) && password.equals(inputPassword);
    }
    return false;
  }
}
