package com.litongjava.tio.http.common;

public interface RequestHeaderKey {
  String Cookie = "cookie"; // Cookie: $Version=1; Skin=new;
  String Origin = "origin"; // http://127.0.0.1
  String Sec_WebSocket_Key = "sec-websocket-key"; // 2GFwqJ1Z37glm62YKKLUeA==
  String Cache_Control = "cache-control"; // "public, max-age=86400"
  String Connection = "connection"; // Upgrade, keep-alive
  String User_Agent = "user-agent"; // Mozilla/5.0 (Windows NT 6.1; WOW64) ...
  String Sec_WebSocket_Version = "sec-websocket-version"; // 13
  String Host = "host"; // 127.0.0.1:9321
  String Pragma = "pragma"; // no-cache
  String Accept_Encoding = "accept-encoding"; // gzip, deflate, br
  String Accept_Language = "accept-language"; // zh-CN,zh;q=0.8,en;q=0.6
  String Upgrade = "upgrade"; // websocket
  String Sec_WebSocket_Extensions = "sec-websocket-extensions"; // permessage-deflate; client_max_window_bits
  String Content_Length = "content-length"; // 65
  String Content_Type = "content-type"; // application/x-www-form-urlencoded; charset=UTF-8
  String If_Modified_Since = "if-modified-since"; // 与Last-Modified配合
  String Referer = "referer";

  /**
   * 值为XMLHttpRequest则为Ajax
   */
  String X_Requested_With = "x-requested-with"; // XMLHttpRequest
  String X_forwarded_For = "x-forwarded-for";
  String Authorization = "authorization";

  String Accept = "accept"; // text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
  String Accept_Charset = "accept-charset"; // utf-8, iso-8859-1;q=0.5
  String Accept_Datetime = "accept-datetime"; // Wed, 21 Oct 2015 07:28:00 GMT
  String DNT = "dnt"; // Do Not Track: 1
  String TE = "te"; // trailers, deflate
  String Upgrade_Insecure_Requests = "upgrade-insecure-requests"; // 1
  String Via = "via"; // 1.0 fred, 1.1 example.com (Apache/1.1)
  String X_Forwarded_Host = "x-forwarded-host"; // original.host.com
  String X_Forwarded_Proto = "x-forwarded-proto"; // https
  String X_Forwarded_Port = "x-forwarded-port"; // 443
  String X_HTTP_Method_Override = "x-http-method-override"; // PUT
  String X_Att_DeviceId = "x-att-deviceid"; // Device ID信息
  String X_UA_Compatible = "x-ua-compatible"; // IE=Edge, chrome=1
  String X_Requested_By = "x-requested-by"; // Custom header indicating request origin
  String X_Csrf_Token = "x-csrf-token"; // CSRF protection token
  String Sec_Fetch_Dest = "sec-fetch-dest"; // document, script, etc.
  String Sec_Fetch_Mode = "sec-fetch-mode"; // navigate, no-cors, cors, etc.
  String Sec_Fetch_Site = "sec-fetch-site"; // same-origin, same-site, cross-site, etc.
  String Sec_Fetch_User = "sec-fetch-user"; // ?1

  // 可选的其他常见请求头
  String Expect = "expect"; // 100-continue
  String Range = "range"; // bytes=500-999
  String If_Range = "if-range"; // "Mon, 21 Oct 2015 07:28:00 GMT" or ETag
  String Connection_Close = "close"; // 关闭连接
}