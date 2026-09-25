package nexus.io.tio.utils.validator;

import org.testng.annotations.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class ParameterValidatorTest {
  @Test public void integerValidationRejectsLossyAndOutOfRangeValues() {
    for (Object value : new Object[] {"1.5", 1.5, "9223372036854775808", "1 or 1=1", -1, null}) {
      try { ParameterValidator.id(value, "id"); fail("Accepted " + value); }
      catch (ParameterValidationException expected) { }
    }
    assertEquals(Long.MAX_VALUE, ParameterValidator.id("9223372036854775807", "id"));
  }

  @Test public void optionalDefaultsAndContainerTypesAreExplicit() {
    assertEquals(20, ParameterValidator.longValue(null, "size", 1, 100, 20L));
    assertNull(ParameterValidator.text("  ", "name", 10, false));
    assertEquals("h5", ParameterValidator.choice(null, "platform", "h5", "h5", "mini"));
    assertTrue(ParameterValidator.array(null, "files", 9).isEmpty());
    assertTrue(ParameterValidator.object(null, "payload").isEmpty());
    try { ParameterValidator.array(Collections.singletonList("x"), "files", 0); fail(); }
    catch (ParameterValidationException expected) { }
    try { ParameterValidator.object("{}", "payload"); fail(); }
    catch (ParameterValidationException expected) { }
  }
}
