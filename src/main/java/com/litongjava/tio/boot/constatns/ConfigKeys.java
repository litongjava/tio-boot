package com.litongjava.tio.boot.constatns;

public class ConfigKeys {
  // server
  public static final String defaultConfigFileName = "app.properties";
  public static final String serverAddress = "server.address";
  public static final String serverPort = "server.port";
  public static final String serverContextPath = "server.context-path";
  public static final String server404 = "server.404";
  public static final String server500 = "server.500";
  public static final String serverResourcesStaticLocations = "server.resources.static-locations";

  // http
  public static final String httpMaxLiveTimeOfStaticRes = "http.maxLiveTimeOfStaticRes";
  public static final String httpUseSession = "http.useSession";
  public static final String httpCheckHost = "http.checkHost";
  public static final String httpMultipartMaxRequestZize = "http.multipart.max-request-size";
  public static final String httpMultipartMaxFileZize = "http.multipart.max-file-size";
  

  // app
  public static final String appEnv = "app.env";
  // jdbc

}
