package com.litongjava.tio.utils.dsn;

import java.net.URI;
import java.net.URISyntaxException;

import com.litongjava.model.dsn.RedisInfo;

public class RedisUrlParser {

  public RedisInfo parse(String redisUrl) {
    URI uri;
    try {
      uri = new URI(redisUrl);
      String host = uri.getHost();
      int port = uri.getPort();
      String userInfo = uri.getUserInfo();

      String password = null;
      if (userInfo != null) {
        String[] userInfoParts = userInfo.split(":", 2);

        if (userInfoParts.length > 1 && !"default".equals(userInfoParts[0])) {
          password = userInfoParts[1];
        }
      }
      return new RedisInfo(host, port, password);
    } catch (URISyntaxException e) {
      new RuntimeException(e);
    }

    return null;
  }
}
