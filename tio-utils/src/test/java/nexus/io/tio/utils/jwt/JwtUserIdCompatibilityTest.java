package nexus.io.tio.utils.jwt;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class JwtUserIdCompatibilityTest {
  @Test
  public void numericAndStringIdsAreReadable() {
    for (Object id : new Object[] { 42L, 42, "42" }) {
      String token = JwtUtils.createTokenByUserId("test-only-signing-key", id);
      assertEquals("42", JwtUtils.parseUserIdString(token));
      assertEquals(Long.valueOf(42), JwtUtils.parseUserIdLong(token));
      assertEquals(Integer.valueOf(42), JwtUtils.parseUserIdInt(token));
    }
  }
  @Test(expectedExceptions = NumberFormatException.class)
  public void integerOverflowIsRejected() {
    String token = JwtUtils.createTokenByUserId("test-only-signing-key", Long.MAX_VALUE);
    JwtUtils.parseUserIdInt(token);
  }
}
