package com.litongjava.tio.utils.token;

public class TimeMillisUtils {

  private static final long ONE_SECOND = 1000L;
  private static final long ONE_MINUTE = ONE_SECOND * 60;
  private static final long ONE_HOUR = ONE_MINUTE * 60;
  private static final long ONE_DAY = ONE_HOUR * 24;
  private static final long ONE_WEEK = ONE_DAY * 7;
  private static final long ONE_MONTH = ONE_DAY * 30; 
  private static final long ONE_YEAR = ONE_DAY * 365;

  public Long getOneHourMillis() {
    return System.currentTimeMillis() + ONE_HOUR;
  }

  public Long getTwoHoursMillis() {
    return System.currentTimeMillis() + 2 * ONE_HOUR;
  }

  public Long getOneDayMillis() {
    return System.currentTimeMillis() + ONE_DAY;
  }

  public Long getTwoDaysMillis() {
    return System.currentTimeMillis() + 2 * ONE_DAY;
  }

  public Long getThreeDaysMillis() {
    return System.currentTimeMillis() + 3 * ONE_DAY;
  }

  public Long getOneWeekMillis() {
    return System.currentTimeMillis() + ONE_WEEK;
  }

  public Long getTwoWeeksMillis() {
    return System.currentTimeMillis() + 2 * ONE_WEEK;
  }

  public Long getOneMonthMillis() {
    return System.currentTimeMillis() + ONE_MONTH;
  }

  public Long getTwoMonthsMillis() {
    return System.currentTimeMillis() + 2 * ONE_MONTH;
  }

  public Long getThreeMonthsMillis() {
    return System.currentTimeMillis() + 3 * ONE_MONTH;
  }

  public Long getOneYearMillis() {
    return System.currentTimeMillis() + ONE_YEAR;
  }

  public Long getTwoYearsMillis() {
    return System.currentTimeMillis() + 2 * ONE_YEAR;
  }

  public Long getThreeYearsMillis() {
    return System.currentTimeMillis() + 3 * ONE_YEAR;
  }

  public Long getOneHundredYearsMillis() {
    return System.currentTimeMillis() + 100 * ONE_YEAR;
  }
}
