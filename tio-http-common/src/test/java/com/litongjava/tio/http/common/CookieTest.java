package com.litongjava.tio.http.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class CookieTest {

  //@Test
  public void testDecode() throws UnsupportedEncodingException {
    String value = "%7B%22status%22%3Atrue%2C%22msg%22%3A%22%u83B7%u53D6%u6210%u529F%21%22%2C%22data%22%3A%7B%22username%22%3A%22199****2980%22%7D%7D";
    String decode = URLDecoder.decode(value, "utf-8");
    System.out.println(decode);
  }

  //@Test
  public void testBuildCookie() {
    Map<String, String> cookieMap = new HashMap<>();
    cookieMap.put("Path", "/www/wwwroot");
    Cookie cookie = Cookie.buildCookie(cookieMap);
    System.out.println(cookie);
    System.out.println(cookie.getName());
    System.out.println(cookie.getValue());

  }
}
