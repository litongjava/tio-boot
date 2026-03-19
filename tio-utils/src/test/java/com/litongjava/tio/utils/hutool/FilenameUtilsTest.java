package com.litongjava.tio.utils.hutool;

import org.junit.Test;

public class FilenameUtilsTest {

  @Test
  public void getSubPath() {
    String filename = "kb/document/md/1.md";
    String subPath = FilenameUtils.getSubPath(filename);
    System.out.println(subPath);
  }

}
