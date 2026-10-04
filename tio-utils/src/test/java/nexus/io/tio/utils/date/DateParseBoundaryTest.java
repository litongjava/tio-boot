package nexus.io.tio.utils.date;

import static org.junit.Assert.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

public class DateParseBoundaryTest {
  @Test
  public void legacyDefaultFormatKeepsUtcAndAcceptsLeapDays() {
    OffsetDateTime expected = OffsetDateTime.parse("2024-02-29T12:00:00Z");
    assertEquals(expected, DateParseUtils.convertToIso8601FromDefault("2024-02-29 12:00:00"));
    assertEquals(expected, DateParseUtils.convertToIso8601Date("2024-02-29 12:00:00"));
    assertEquals(expected, DateParseUtils.convertToIso8601FromDefault(
        Arrays.<Object>asList("2024-02-29 12:00:00")).get(0));
  }

  @Test
  public void listConversionPreservesOffsetAndInstant() {
    List<OffsetDateTime> dates = DateParseUtils.convertToIso8601Date(Arrays.<Object>asList(
        "2026-10-04T08:30:00+08:00", "2026-10-03T19:00:00-05:30"));
    assertEquals(ZoneOffset.ofHours(8), dates.get(0).getOffset());
    assertEquals(Instant.parse("2026-10-04T00:30:00Z"), dates.get(0).toInstant());
    assertEquals(dates.get(0).toInstant(), dates.get(1).toInstant());
  }

  @Test
  public void isoParserSupportsOptionalFractionsAndOffsets() {
    Instant expected = Instant.parse("2026-10-04T00:30:00Z");
    assertEquals(expected, DateParseUtils.parseIso8601Date("2026-10-04T00:30:00Z").toInstant());
    assertEquals(expected, DateParseUtils.parseIso8601Date("2026-10-04T08:30:00+08:00").toInstant());
    assertEquals(123, DateParseUtils.parseIso8601Date("1970-01-01T00:00:00.123456Z").getTime());
  }

  @Test
  public void invalidDatesAndTrailingGarbageAreRejected() {
    assertNull(DateParseUtils.parseIso8601Date("2026-02-30T00:00:00.000Z"));
    assertNull(DateParseUtils.parseIso8601Date("2026-10-04T00:00:00.000Zgarbage"));
    assertNull(DateParseUtils.parseIso8601Date(null));
    try {
      DateParseUtils.convertToIso8601FromDefault("2026-02-30 00:00:00");
      fail("Invalid calendar dates must not be silently normalized");
    } catch (DateTimeParseException expected) {
      // Strict date validation.
    }
  }
}
