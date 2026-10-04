package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.util.Locale;
import org.testng.annotations.Test;

public class StringRandomBoundaryTest {
  @Test
  public void backwardSearchIncludesFirstCharacter() {
    assertEquals(0, StrUtil.lastIndexOfIgnoreCase("Abc", "a"));
    assertEquals(0, StrUtil.lastIndexOfIgnoreCase(new StringBuilder("Abc"), "ABC", 0));
  }

  @Test
  public void searchOverloadsHandleNullConsistently() {
    assertEquals(-1, StrUtil.lastIndexOfIgnoreCase(null, "a"));
    assertEquals(-1, StrUtil.indexOf(null, 'a'));
    assertEquals(-1, StrUtil.indexOf(null, 'a', 0, 3));
  }

  @Test
  public void characterSearchDoesNotRestartPastEnd() {
    assertEquals(-1, StrUtil.indexOf(new StringBuilder("abc"), 'a', 4));
    assertEquals(-1, StrUtil.indexOf("abc", 'a', 4, 8));
    assertEquals(0, StrUtil.indexOf(new StringBuilder("abc"), 'a', -1));
    assertEquals(-1, StrUtil.indexOf("abc", 'c', 0, 2));
  }

  @Test
  public void substringSearchMatchesStringBoundarySemantics() {
    String[] haystacks = {"", "a", "AbA", "abc"};
    String[] needles = {"", "a", "A", "ab", "abcde"};
    int[] starts = {-2, 0, 1, 3, 4, Integer.MAX_VALUE};
    for (String haystack : haystacks) {
      for (String needle : needles) {
        for (int start : starts) {
          for (boolean ignoreCase : new boolean[] {false, true}) {
            String expectedText = ignoreCase ? haystack.toLowerCase(Locale.ROOT) : haystack;
            String expectedNeedle = ignoreCase ? needle.toLowerCase(Locale.ROOT) : needle;
            assertEquals(expectedText.indexOf(expectedNeedle, start),
                StrUtil.indexOf(new StringBuilder(haystack), needle, start, ignoreCase));
            assertEquals(expectedText.lastIndexOf(expectedNeedle, start),
                StrUtil.lastIndexOf(new StringBuilder(haystack), needle, start, ignoreCase));
          }
        }
      }
    }
  }

  @Test
  public void randomIntegerAcceptsWideInclusiveRanges() {
    int[][] ranges = {{Integer.MIN_VALUE, Integer.MAX_VALUE}, {0, Integer.MAX_VALUE},
        {Integer.MIN_VALUE, 0}, {-100, 100}, {Integer.MAX_VALUE - 1, Integer.MAX_VALUE}};
    for (int[] range : ranges) {
      for (int i = 0; i < 256; i++) {
        int value = RandomUtils.nextInt(range[0], range[1]);
        assertTrue(value >= range[0] && value <= range[1]);
      }
    }
  }

  @Test
  public void singletonRandomRangeReturnsItsOnlyValue() {
    for (int value : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
      assertEquals(value, RandomUtils.nextInt(value, value));
    }
  }

  @Test
  public void invertedRandomRangeIsAlwaysRejected() {
    int[][] ranges = {{1, 0}, {Integer.MAX_VALUE, Integer.MIN_VALUE}};
    for (int[] range : ranges) {
      try {
        RandomUtils.nextInt(range[0], range[1]);
        fail("Inverted bounds must be rejected");
      } catch (IllegalArgumentException expected) {
        assertNotNull(expected.getMessage());
      }
    }
  }
}
