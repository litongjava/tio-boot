package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.util.regex.Pattern;
import org.testng.annotations.Test;

public class RegexTemplateBoundaryTest {
  @Test
  public void multiDigitGroupsAreIndependent() {
    assertEquals("j-a-j", ReUtil.replaceAll("abcdefghij", "(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)", "$10-$1-$10"));
  }

  @Test
  public void capturedGroupReferencesRemainLiteral() {
    assertEquals("$2-X", ReUtil.replaceAll("$2:X", "(.*):(.*)", "$1-$2"));
  }

  @Test
  public void unmatchedOptionalGroupsProduceEmptyText() {
    assertEquals("[b||b]", ReUtil.replaceAll("b", "(a)?(b)", "[$0|$1|$2]"));
  }

  @Test
  public void replacementAndCapturedPunctuationRemainLiteral() {
    String value = "C:\\tmp\\$5";
    assertEquals("prefix\\" + value + "$suffix", ReUtil.replaceAll(value, Pattern.compile("(.+)"), "prefix\\$1$suffix"));
  }

  @Test
  public void multipleMatchesAndNoMatchesRetainTheirContract() {
    assertEquals("<1> <2>", ReUtil.replaceAll("a1 a2", "a([0-9])", "<$1>"));
    assertEquals("abc", ReUtil.replaceAll("abc", "[0-9]", "$1"));
    assertNull(ReUtil.replaceAll(null, Pattern.compile("."), "x"));
    assertEquals("", ReUtil.replaceAll("", Pattern.compile("."), "x"));
  }

  @Test
  public void whitespaceCanBeDeletedByPattern() {
    Pattern whitespace = Pattern.compile("\\s");
    assertEquals("", ReUtil.delAll(whitespace, " \t\n"));
    assertEquals("\t\n", ReUtil.delFirst(whitespace, " \t\n"));
  }

  @Test
  public void deletionPreservesNullAndUnmatchedInputs() {
    Pattern digits = Pattern.compile("[0-9]");
    assertNull(ReUtil.delAll(digits, null));
    assertNull(ReUtil.delFirst(digits, null));
    assertEquals("", ReUtil.delAll(digits, ""));
    assertEquals("abc", ReUtil.delFirst(digits, "abc"));
    assertEquals("abc", ReUtil.delAll(null, "abc"));
    assertEquals("ac", ReUtil.delAll(digits, "a1c2"));
    assertEquals("ac2", ReUtil.delFirst(digits, "a1c2"));
  }
}
