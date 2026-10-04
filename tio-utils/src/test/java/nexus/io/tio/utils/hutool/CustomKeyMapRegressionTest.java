package nexus.io.tio.utils.hutool;

import static org.junit.Assert.*;
import java.util.*;
import org.testng.annotations.Test;
import nexus.io.tio.utils.BinaryUtils;

public class CustomKeyMapRegressionTest {
  @Test public void allKeyOperationsUseTheSameIdentity() {
    CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
    map.put("KEY", 1);
    assertEquals(Integer.valueOf(1), map.getOrDefault("KEY", -1));
    assertEquals(Integer.valueOf(1), map.putIfAbsent("Key", 99));
    assertEquals(Integer.valueOf(1), map.computeIfAbsent("KEY", k -> 99));
    assertEquals(Integer.valueOf(2), map.computeIfPresent("KEY", (k,v) -> v + 1));
    assertEquals(Integer.valueOf(3), map.compute("KEY", (k,v) -> v + 1));
    assertEquals(Integer.valueOf(4), map.merge("KEY", 1, Integer::sum));
    assertEquals(Integer.valueOf(4), map.replace("KEY", 5));
    assertTrue(map.replace("KEY", 5, 6));
    assertEquals(1, map.size());
    assertTrue(map.keySet().contains("KEY"));
    assertTrue(map.entrySet().contains(new AbstractMap.SimpleEntry<>("KEY", 6)));
    assertFalse(map.remove("KEY", 9));
    assertTrue(map.remove("KEY", 6));
    map.put("Key", null);
    assertTrue(map.keySet().remove("KEY"));
    map.put("Key", 7);
    assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>("KEY", 7)));
    map.put("Key", 8);
    assertEquals(Integer.valueOf(8), map.remove("KEY"));
    assertTrue(map.isEmpty());
    map.put(null, 1);
    assertEquals(Integer.valueOf(1), map.getOrDefault(null, -1));
  }

  @Test public void casingDoesNotDependOnDefaultLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
      map.put("TITLE", 1);
      assertEquals(Integer.valueOf(1), map.get("title"));
      Locale.setDefault(Locale.US);
      assertEquals(Integer.valueOf(1), map.get("TITLE"));
    } finally { Locale.setDefault(previous); }
  }

  @Test public void inviteCodesRoundTripAndRejectMalformedInput() {
    for (int id : new int[] {0, 1, 30, 31, 9999, Integer.MAX_VALUE}) {
      assertEquals(id, BinaryUtils.decode(BinaryUtils.encode(id)));
    }
    for (String code : new String[] {null, "", "!", "b!", "z", "bz!", "bzz", "yyyyyyyyyyyyyyyyyyyy"}) {
      try { BinaryUtils.decode(code); fail("Accepted invalid code: " + code); }
      catch (IllegalArgumentException expected) { }
    }
    try { BinaryUtils.encode(-1); fail("Accepted negative ID"); }
    catch (IllegalArgumentException expected) { }
  }
}
