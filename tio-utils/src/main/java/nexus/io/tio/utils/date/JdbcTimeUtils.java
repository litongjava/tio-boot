package nexus.io.tio.utils.date;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

/** Convert timestamp values returned by JDBC drivers without losing fractional seconds. */
public final class JdbcTimeUtils {
  private JdbcTimeUtils() {
  }

  public static Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant) {
      return (Instant) value;
    }
    if (value instanceof Timestamp) {
      return ((Timestamp) value).toInstant();
    }
    if (value instanceof OffsetDateTime) {
      return ((OffsetDateTime) value).toInstant();
    }
    throw new IllegalArgumentException("Expected an Instant, Timestamp or OffsetDateTime");
  }
}
