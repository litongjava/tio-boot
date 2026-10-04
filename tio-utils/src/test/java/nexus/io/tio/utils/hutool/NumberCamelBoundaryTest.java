package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.text.ParseException;
import java.util.Locale;
import org.testng.annotations.Test;

public class NumberCamelBoundaryTest {
  @Test
  public void numericConversionRejectsUnconsumedSuffix() throws Exception {
    for (String input : new String[] {"123abc", "12 34", "42x"}) {
      try {
        StrUtil.convert(Number.class, input);
        fail("A numeric conversion must consume the complete input");
      } catch (ParseException expected) {
        assertTrue(expected.getErrorOffset() >= 0);
      }
    }
  }

  @Test
  public void localeSpecificNumericFormatsRemainSupported() throws Exception {
    Locale previous = Locale.getDefault();
    Locale previousFormat = Locale.getDefault(Locale.Category.FORMAT);
    try {
      Locale.setDefault(Locale.US);
      Locale.setDefault(Locale.Category.FORMAT, Locale.US);
      assertEquals(1234.5, ((Number) StrUtil.convert(Number.class, "1,234.5")).doubleValue(), 0.0);
      Locale.setDefault(Locale.GERMANY);
      Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
      assertEquals(1234.5, ((Number) StrUtil.convert(Number.class, "1.234,5")).doubleValue(), 0.0);
    } finally {
      Locale.setDefault(previous);
      Locale.setDefault(Locale.Category.FORMAT, previousFormat);
    }
  }

  @Test
  public void numericBlankAndArrayBehaviorIsPreserved() throws Exception {
    assertNull(StrUtil.convert(Number.class, (String) null));
    assertNull(StrUtil.convert(Number.class, " "));
    Number[] values = (Number[]) StrUtil.convert(Number[].class, new String[] {"1", "", "2"});
    assertEquals(1L, values[0].longValue());
    assertNull(values[1]);
    assertEquals(2L, values[2].longValue());
    assertEquals(Integer.valueOf(42), StrUtil.convert(Integer.class, "42"));
  }

  @Test
  public void singleCharacterHonorsLowercaseFlag() {
    assertEquals("a", StrUtil.toCamelCase("A", true));
    assertEquals("A", StrUtil.toCamelCase("A", false));
    assertEquals("a", StrUtil.toCamelCase("a", true));
    assertEquals("1", StrUtil.toCamelCase("1", true));
    assertEquals("", StrUtil.toCamelCase("", true));
  }

  @Test
  public void longerNamesRetainCamelCaseConventions() {
    assertEquals("userId", StrUtil.toCamelCase("USER_ID", false));
    assertEquals("userId", StrUtil.toCamelCase("USER_ID", true));
    assertEquals("userId", StrUtil.toCamelCase("userId", false));
    assertEquals("userid", StrUtil.toCamelCase("userId", true));
  }
}
