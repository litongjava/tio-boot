package com.litongjava.tio.utils.http.useragent;

import org.junit.Test;

import com.litongjava.model.http.useragent.UserAgent;
import com.litongjava.tio.utils.json.JsonUtils;

public class UserAgentUtilTest {

  @Test
  public void test() {
    String string="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36";
    UserAgent userAgent = UserAgentUtil.parse(string);
    System.out.println(JsonUtils.toJson(userAgent));
  }

}
