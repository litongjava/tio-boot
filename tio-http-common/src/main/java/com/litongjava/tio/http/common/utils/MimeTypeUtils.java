package com.litongjava.tio.http.common.utils;

import com.litongjava.tio.http.common.MimeType;

public class MimeTypeUtils {

  public static String APPLICATION_JSON_UTF_8 = MimeType.APPLICATION_JSON.getType() + ";charset=UTF-8";
  public static String TEXT_PLAIN_UTF_8 = MimeType.TEXT_PLAIN_TXT.getType() + ";charset=UTF-8";

  public static String getMimeTypeStr(MimeType mimeType, String charset) {
    if (charset == null) {
      return mimeType.getType();
    } else {
      return mimeType.getType() + ";charset=" + charset;
    }
  }

  public static String getTextUTF8() {
    return TEXT_PLAIN_UTF_8;
  }

  public static String getJsonUTF8() {
    return APPLICATION_JSON_UTF_8;
  }

  public static String getJson(String charset) {
    if (charset == null) {
      return MimeType.APPLICATION_JSON.getType();
    } else {
      return MimeType.APPLICATION_JSON.getType() + ";charset=" + charset;
    }
  }

  public static String getText(String charset) {
    if (charset == null) {
      return MimeType.TEXT_PLAIN_TXT.getType();
    } else {
      return MimeType.TEXT_PLAIN_TXT.getType() + ";charset=" + charset;
    }
  }

}
