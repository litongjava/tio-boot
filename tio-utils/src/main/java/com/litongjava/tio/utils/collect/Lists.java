package com.litongjava.tio.utils.collect;

import java.util.ArrayList;
import java.util.List;

public class Lists {

  public static List<String> of(String... elements) {
    List<String> arrayList = new ArrayList<>(elements.length);

    for (String e : elements) {
      arrayList.add(e);
    }
    return arrayList;
  }

}
