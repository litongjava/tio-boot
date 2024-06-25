package com.litongjava.tio.boot.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

  public static List<OffsetDateTime> convertToIso8601Date(List<Object> list) {
    List<OffsetDateTime> retval = new ArrayList<>(list.size());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    for (Object object : list) {
      // Parse the input strings to LocalDateTime
      LocalDateTime localTime = LocalDateTime.parse((String) object, formatter);
      OffsetDateTime offsetDateTime = localTime.atOffset(ZoneOffset.UTC);
      retval.add(offsetDateTime);
    }

    return retval;
  }

  public static List<OffsetDateTime> convertToIso8601FromDefault(List<Object> list) {
    List<OffsetDateTime> retval = new ArrayList<>(list.size());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    for (Object object : list) {
      // Parse the input strings to LocalDateTime
      LocalDateTime localTime = LocalDateTime.parse((String) object, formatter);
      OffsetDateTime offsetDateTime = localTime.atOffset(ZoneOffset.UTC);
      retval.add(offsetDateTime);
    }

    return retval;
  }
}
