package com.litongjava.tio.utils.markdown;

public class MdUtils {

  public static String code(String languageName, String string) {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("\n");
    stringBuffer.append("```").append(languageName).append("\n");
    stringBuffer.append(string).append("\n");
    stringBuffer.append("```").append("\n");
    return stringBuffer.toString();
  }
}
