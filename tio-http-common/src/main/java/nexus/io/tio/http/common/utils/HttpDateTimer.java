package nexus.io.tio.http.common.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.utils.SystemTimer;

/** Cached HTTP date, regenerated only when the UTC second changes. */
public class HttpDateTimer {
  private static final DateTimeFormatter FORMAT = DateTimeFormatter
      .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).withZone(ZoneOffset.UTC);
  private static volatile long second = System.currentTimeMillis() / 1000;
  private static volatile String httpDateString = FORMAT.format(Instant.ofEpochSecond(second));
  public static volatile HeaderValue httpDateValue = HeaderValue.from(httpDateString);

  static {
    SystemTimer.addTimerListener(HttpDateTimer::update);
  }

  static synchronized void update(long currTime) {
    long currentSecond = currTime / 1000;
    if (currentSecond == second) return;
    String date = FORMAT.format(Instant.ofEpochSecond(currentSecond));
    httpDateString = date;
    httpDateValue = HeaderValue.from(date);
    second = currentSecond;
  }

  public static String currDateString() { return httpDateString; }
  public static HeaderValue httpDateValue() { return httpDateValue; }
}
