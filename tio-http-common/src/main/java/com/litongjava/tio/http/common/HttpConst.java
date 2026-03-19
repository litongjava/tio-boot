package com.litongjava.tio.http.common;

/**
 * @author tanyaowu
 */
public interface HttpConst {

  public interface HttpVersion {
    String V1_1 = "1.1";
    String V1_0 = "1.0";
  }

  /**
   * 请求体的格式
   * @author tanyaowu
   * 2017年6月28日 上午10:03:12
   */
  public enum RequestBodyFormat {
    URLENCODED, MULTIPART, TEXT
  }

  /**
   *         Accept-Language : zh-CN,zh;q=0.8
       Sec-WebSocket-Version : 13
    Sec-WebSocket-Extensions : permessage-deflate; client_max_window_bits
                     Upgrade : websocket
                        Host : t-io.org:9321
             Accept-Encoding : gzip, deflate, sdch
                  User-Agent : Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
                      Origin : http://www.t-io.org:9292
           Sec-WebSocket-Key : kmCL2C7q9vtNSMyHpft7lw==
                  Connection : Upgrade
               Cache-Control : no-cache
                      Pragma : no-cache
   */

  public interface RequestHeaderValue {
    public interface Connection {
      String keep_alive = "keep-alive"; // .toLowerCase();
      String Upgrade = "upgrade"; // .toLowerCase();
      String close = "close"; // .toLowerCase();
    }

    // application/x-www-form-urlencoded、multipart/form-data、text/plain
    public interface Content_Type {
      /**
       * 普通文本，一般会是json或是xml
       */
      String text_plain = "text/plain";
      /**
       * 文件上传
       */
      String multipart_form_data = "multipart/form-data";
      /**
       * 普通的key-value
       */
      String application_x_www_form_urlencoded = "application/x-www-form-urlencoded";
    }
  }

  public interface ResponseHeaderValue {
    public interface Connection {
      String keep_alive = "keep-alive"; // .toLowerCase();
      String Upgrade = "Upgrade"; // .toLowerCase();
      String close = "close"; // .toLowerCase();
    }

    public interface Upgrade {
      String WebSocket = "WebSocket";
    }
  }

  String SERVER_INFO = "t-io";

}
