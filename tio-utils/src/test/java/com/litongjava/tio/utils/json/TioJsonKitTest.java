package com.litongjava.tio.utils.json;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TioJsonKitTest {

  @Test
  public void test() {
    Map<String, Object> map = new HashMap<>();
    map.put("long", 1L);

    String json = JsonUtils.toJson(map);
    System.out.println(json);
  }

}
