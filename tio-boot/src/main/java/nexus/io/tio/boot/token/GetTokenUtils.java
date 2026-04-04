package nexus.io.tio.boot.token;

import nexus.io.tio.http.common.HttpRequest;

public class GetTokenUtils {

  public static String getToken(HttpRequest request) {
    String token = request.getParam("token");
    if (token == null) {
      token = request.getHeader("token");
    }
    if (token == null) {
      String authorization = request.getAuthorization();
      if (authorization != null) {
        String[] split = authorization.split(" ");

        if (split.length > 1) {
          token = split[1];
        } else {
          token = split[0];
        }
      }
    }
    return token;
  }
}
