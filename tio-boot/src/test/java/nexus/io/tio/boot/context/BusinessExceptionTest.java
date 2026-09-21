package nexus.io.tio.boot.context;

import nexus.io.tio.boot.exception.BusinessException;
import org.junit.Test;
import static org.junit.Assert.*;

public class BusinessExceptionTest {
  @Test public void preservesStatusMessageAndCause() {
    RuntimeException cause = new RuntimeException("internal");
    BusinessException error = new BusinessException(409, "Conflict", cause);
    assertEquals(409, error.getStatus());
    assertEquals("Conflict", error.getMessage());
    assertSame(cause, error.getCause());
    try { new BusinessException(200, "Invalid"); fail(); }
    catch (IllegalArgumentException expected) { }
  }

  @Test public void requireSupportsDefaultAndExplicitStatus() {
    BusinessException.require(true, "Valid");
    try { BusinessException.require(false, "Invalid"); fail(); }
    catch (BusinessException error) { assertEquals(400, error.getStatus()); }
    try { BusinessException.require(false, 403, "Forbidden"); fail(); }
    catch (BusinessException error) { assertEquals(403, error.getStatus()); }
  }
}
