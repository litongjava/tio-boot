package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.util.Date;
import java.util.TimeZone;
import org.testng.annotations.Test;

public class DateUtilBoundaryTest {
  @Test
  public void httpDateUsesGmtRegardlessOfDefaultZone() {
    TimeZone previous = TimeZone.getDefault();
    try {
      for (String zone : new String[] {"Asia/Shanghai", "America/Los_Angeles", "UTC"}) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", DateUtil.httpDate(0L));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", DateUtil.httpDate(new Date(0L)));
      }
    } finally {
      TimeZone.setDefault(previous);
    }
  }

  @Test
  public void nullAndBlankDatesAreAbsentAcrossOverloads() {
    for (String input : new String[] {null, "", "   "}) {
      assertNull(DateUtil.guessPattern(input));
      assertNull(DateUtil.parseToDate(input));
      assertNull(DateUtil.parseToSqlDate(input));
      assertNull(DateUtil.parseToTimestamp(input));
      assertNull(DateUtil.parseToTime(input));
    }
  }

  @Test
  public void invalidCalendarFieldsAreNotNormalized() throws Exception {
    for (String input : new String[] {"2025-02-29", "2024-02-30", "2024-13-01", "2024-01-00",
        "2024-01-01 24:00:00", "25:00:00", "2024-01-01 12:60:00"}) {
      assertNull(input, DateUtil.parseToDate(input));
      assertNull(input, StrUtil.convert(Date.class, input));
    }
  }

  @Test
  public void trailingContentIsNotIgnored() {
    assertNull(DateUtil.parseToDate("2024-01-01 12:34:56x"));
    assertNull(DateUtil.parseToDate("2024-01-01 12:34:56.123junk"));
  }

  @Test
  public void whitespaceIsNormalizedBeforeParsing() {
    assertEquals(DateUtil.parseToDate("2024-02-29"), DateUtil.parseToDate(" 2024-02-29 "));
    assertEquals(DateUtil.parseToDate("20240229"), DateUtil.parseToDate(" 20240229 "));
  }

  @Test
  public void documentedPatternsAndSqlWrappersRetainValues() {
    String[] inputs = {"2024-02-29", "20240229", "12:34:56", "123456", "2024-02-29 12:34",
        "2024-02-29 12:34:56", "20240229123456", "2024-02-29 12:34:56.123", "20240229123456123"};
    for (String input : inputs) {
      Date date = DateUtil.parseToDate(input);
      assertNotNull(input, date);
      assertEquals(date.getTime(), DateUtil.parseToSqlDate(input).getTime());
      assertEquals(date.getTime(), DateUtil.parseToTimestamp(input).getTime());
      assertEquals(date.getTime(), DateUtil.parseToTime(input).getTime());
    }
  }
}
