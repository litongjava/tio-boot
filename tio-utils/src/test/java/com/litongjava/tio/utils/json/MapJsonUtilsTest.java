package com.nexus.tio.utils.json;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class MapJsonUtilsTest {

  @Test
  public void testToPrettyJson() {
    // Create a map
    Map<String, String> map = new LinkedHashMap<>();
    map.put("/", "com.nexus.tio.web.hello.controller.IndexController");
    map.put("/aop", "com.nexus.tio.web.hello.controller.AopController");

    // Convert the map to a pretty JSON string
    String prettyJson = MapJsonUtils.toPrettyJson(map);

    // Print the pretty JSON string
    System.out.println(prettyJson);
  }

}
