package nexus.io.tio.utils;

import nexus.io.tio.utils.environment.EnvUtils;

public class DomainUtils {

  public static String append(String endpoint) {
    boolean isHttps = EnvUtils.getBoolean("app.https", true);
    String domain = EnvUtils.getStr("app.domain");
    String apiPrefixUrl;
    if (isHttps) {
      apiPrefixUrl = "https://" + domain + endpoint;
    } else {
      apiPrefixUrl = "http://" + domain + endpoint;
    }
    return apiPrefixUrl;
  }

}
