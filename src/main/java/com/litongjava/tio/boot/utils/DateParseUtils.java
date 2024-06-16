package com.litongjava.tio.boot.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateParseUtils {

  /**
   * Parses an ISO 8601 date string to a java.util.Date object.
   *
   * @param dateString The ISO 8601 date string.
   * @return The parsed Date object.
   */
  public static Date parseIso8601Date(String dateString) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC")); // Set the formatter to UTC
    try {
      return formatter.parse(dateString);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }
}
