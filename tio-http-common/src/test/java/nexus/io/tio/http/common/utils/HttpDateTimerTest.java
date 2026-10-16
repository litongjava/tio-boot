package nexus.io.tio.http.common.utils;

import static org.testng.Assert.*;
import java.time.Instant;
import org.testng.annotations.Test;
import nexus.io.tio.http.common.HeaderValue;

public class HttpDateTimerTest {
  @Test public void reusesHeaderWithinOneSecondAndFormatsUtc() {
    // Serialize with the background clock while testing specific timestamps.
    synchronized (HttpDateTimer.class) {
      long epoch = Instant.parse("2026-10-06T03:04:05Z").toEpochMilli();
      try {
        HttpDateTimer.update(epoch);
        HeaderValue first = HttpDateTimer.httpDateValue();
        assertEquals(HttpDateTimer.currDateString(), "Tue, 06 Oct 2026 03:04:05 GMT");
        HttpDateTimer.update(epoch + 999); assertSame(HttpDateTimer.httpDateValue(), first);
        HttpDateTimer.update(epoch + 1000); assertNotSame(HttpDateTimer.httpDateValue(), first);
        assertEquals(HttpDateTimer.currDateString(), "Tue, 06 Oct 2026 03:04:06 GMT");
      } finally { HttpDateTimer.update(System.currentTimeMillis()); }
    }
  }
}
