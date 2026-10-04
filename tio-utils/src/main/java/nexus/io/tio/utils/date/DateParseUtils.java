package nexus.io.tio.utils.date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DateParseUtils {
  private static final DateTimeFormatter DEFAULT_DATE_TIME = DateTimeFormatter
      .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

  /**
   * Parses an ISO 8601 date string to a java.util.Date object.
   *
   * @param dateString The ISO 8601 date string.
   * @return The parsed Date object.
   */
  public static Date parseIso8601Date(String dateString) {
    if (dateString == null) {
      return null;
    }
    try {
      return Date.from(OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public static List<OffsetDateTime> convertToIso8601Date(List<Object> list) {
    List<OffsetDateTime> retval = new ArrayList<>(list.size());
    for (Object object : list) {
      retval.add(OffsetDateTime.parse((String) object, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    return retval;
  }

  public static List<OffsetDateTime> convertToIso8601FromDefault(List<Object> list) {
    List<OffsetDateTime> retval = new ArrayList<>(list.size());
    DateTimeFormatter formatter = DEFAULT_DATE_TIME;

    for (Object object : list) {
      // Parse the input strings to LocalDateTime
      LocalDateTime localTime = LocalDateTime.parse((String) object, formatter);
      OffsetDateTime offsetDateTime = localTime.atOffset(ZoneOffset.UTC);
      retval.add(offsetDateTime);
    }

    return retval;
  }

  public static OffsetDateTime convertToIso8601Date(String inputValue) {
    DateTimeFormatter formatter = DEFAULT_DATE_TIME;
    LocalDateTime localTime = LocalDateTime.parse(inputValue, formatter);
    OffsetDateTime offsetDateTime = localTime.atOffset(ZoneOffset.UTC);
    return offsetDateTime;
  }

  public static OffsetDateTime convertToIso8601FromDefault(String inputValue) {
    DateTimeFormatter formatter = DEFAULT_DATE_TIME;
    LocalDateTime localTime = LocalDateTime.parse(inputValue, formatter);
    OffsetDateTime offsetDateTime = localTime.atOffset(ZoneOffset.UTC);
    return offsetDateTime;
  }

  public static OffsetDateTime convertToIso8601FromSecond(Long seconds) {
    Instant instant = Instant.ofEpochSecond(seconds);
    ZoneId zoneId = ZoneId.systemDefault();
    return instant.atZone(zoneId).toOffsetDateTime();
  }

  public static OffsetDateTime convertToIso8601FromSecond(String inputValue) {
    long seconds = Long.parseLong(inputValue);
    return convertToIso8601FromSecond(seconds);
  }

  public static OffsetDateTime convertToIso8601Frommillisecond(String inputValue) {
    long milliseconds = Long.parseLong(inputValue);
    return convertToIso8601Frommillisecond(milliseconds);
  }

  public static OffsetDateTime convertToIso8601Frommillisecond(Long milliseconds) {
    Instant instant = Instant.ofEpochMilli(milliseconds);
    ZoneId zoneId = ZoneId.systemDefault();
    return instant.atZone(zoneId).toOffsetDateTime();
  }

  /**
   * 
   * @param inputValue eg:2024-05-20 06:53:58 +0000 UTC
   * @return
   */
  public static OffsetDateTime parseUTCDateString(String inputValue) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z z");
    return OffsetDateTime.parse(inputValue, formatter);
  }
}
