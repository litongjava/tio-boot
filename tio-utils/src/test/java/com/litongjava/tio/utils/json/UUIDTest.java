package com.litongjava.tio.utils.json;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class UUIDTest {
  @Test
  public void test() {
    Map<String,Object> map=new HashMap<>();
    map.put("id", UUID.randomUUID());
    
    System.out.println(JsonUtils.toJson(map));
  }

}
