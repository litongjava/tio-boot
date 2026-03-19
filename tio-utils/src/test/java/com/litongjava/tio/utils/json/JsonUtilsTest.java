package com.litongjava.tio.utils.json;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class JsonUtilsTest {

  @Test
  public void testToJsonBytes() {
    Map<String, Object> map = new HashMap<>();
    map.put("age", 18);
    map.put("name", "Tong Li");
    byte[] jsonBytes = JsonUtils.toJsonBytes(map);
    System.out.println(new String(jsonBytes));
  }

}
