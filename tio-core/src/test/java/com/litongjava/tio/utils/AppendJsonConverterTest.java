package com.litongjava.tio.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class AppendJsonConverterTest {

  @Test
  public void testConvertListToJson() {
    List<Long> lists=new ArrayList<>();
    lists.add(001L);
    lists.add(002L);
    
    String convertListToJson = AppendJsonConverter.convertListLongToJson(lists);
    System.out.println(convertListToJson);
    
        
  }

}
