package com.litongjava.tio.utils.json;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ParseToListMapTest {

  @Test
  public void test01() {
    String str = "[{\"uid\":\"1719293770079\",\"name\":\"image.png\",\"status\":\"done\",\"size\":477120,\"type\":\"image/png\",\"id\":\"395177974162264064\",\"url\":\"https://rumiapp.s3.us-west-1.amazonaws.com/sjsj/professors/395177969997320192.png\"}]";
    List<Map<String, Object>> list = JsonUtils.parseToListMap(str, String.class, Object.class);
    System.out.println(list);
  }

}
