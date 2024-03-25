package com.litongjava.tio.boot.constatns;

public class TioBootConfigKeys {
  // server
  public static final String DEFAULT_CONFIG_FILE_NAME = "app.properties";
  public static final String SERVER_ADDRESS = "server.address";
  public static final String SERVER_PORT = "server.port";
  public static final String SERVER_CONTEXT_PATH = "server.context-path";
  public static final String SERVER_404 = "server.404";
  public static final String SERVER_500 = "server.500";
  public static final String SERVER_RESOURCES_STATIC_LOCATIONS = "server.resources.static-locations";

  // http
  public static final String HTTP_MAX_LIVE_TIME_OF_STATIC_RES = "http.max.live.time.of.static.res";
  public static final String HTTP_CHECK_HOST = "http.checkHost";
  public static final String HTTP_MULTIPART_MAX_REQUEST_SIZE = "http.multipart.max-request-size";
  public static final String HTTP_MULTIPART_MAX_FILE_ZIZE = "http.multipart.max-file-size";
  public static final String HTTP_ENABLE_SESSION = "http.enable.session";
  public static final String HTTP_ENABLE_REQUEST_LIMIT = "http.enable.request.limit";
  public static final String HTTP_MAX_REQUESTS_PER_SECOND = "http.max.requests.per.second";

  // tio
  public static final String TIO_DEV_MODE = "tio.dev.mode";
  public static final String TIO_NO_SERVER = "tio.no.server";
  public static final String TIO_HTTP_REQUEST_PRINT_URL ="tio.http.request.printUrl";

  // app
  public static final String APP_ENV = "app.env";
  public static final String APP_NAME = "app.name";
  

  // jdbc
  
  // aop
  public static final String AOP_PRINT_SCANNED_CLASSSES = "aop.print.scanned.classes";
  

}
