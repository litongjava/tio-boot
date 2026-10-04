package nexus.io.tio.utils.json;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import org.testng.annotations.Test;

public class JsonGenericBoundaryTest {
  public static class Item {
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  private Map<Integer, Item> map(int backend, String json) {
    switch (backend) {
    case 0: return new FastJson2().parseToMap(json, Integer.class, Item.class);
    case 1: return FastJson2Utils.parseToMap(json, Integer.class, Item.class);
    case 2: return new Jackson().parseToMap(json, Integer.class, Item.class);
    default: return JacksonUtils.parseToMap(json, Integer.class, Item.class);
    }
  }

  private List<Map<Integer, Item>> list(int backend, String json) {
    switch (backend) {
    case 0: return new FastJson2().parseToListMap(json, Integer.class, Item.class);
    case 1: return FastJson2Utils.parseToListMap(json, Integer.class, Item.class);
    case 2: return new Jackson().parseToListMap(json, Integer.class, Item.class);
    default: return JacksonUtils.parseToListMap(json, Integer.class, Item.class);
    }
  }

  @Test
  public void typedMapsHonorBothKeyAndBeanTypes() {
    for (int backend = 0; backend < 4; backend++) {
      Map<Integer, Item> result = map(backend, "{\"7\":{\"name\":\"value\"}}");
      assertTrue("backend=" + backend, result.containsKey(7));
      assertEquals("value", result.get(7).getName());
    }
  }

  @Test
  public void typedListMapsHonorBothTypesAndPreserveNullElements() {
    for (int backend = 0; backend < 4; backend++) {
      List<Map<Integer, Item>> result = list(backend, "[null,{\"7\":{\"name\":\"value\"}},{}]");
      assertEquals(3, result.size());
      assertNull(result.get(0));
      assertTrue("backend=" + backend, result.get(1).containsKey(7));
      assertEquals("value", result.get(1).get(7).getName());
      assertTrue(result.get(2).isEmpty());
    }
  }

  @Test
  public void jsonNullAndEmptyArrayKeepTheirDifferentMeanings() {
    for (int backend = 0; backend < 4; backend++) {
      assertNull(list(backend, "null"));
      assertTrue(list(backend, "[]").isEmpty());
    }
  }

  @Test
  public void jacksonNullOutputCanBeReenabledWithoutContaminatingSharedMapper() throws Exception {
    Map<String, String> value = Collections.singletonMap("missing", null);
    Boolean previous = JacksonUtils.generateNullValue;
    try {
      JacksonUtils.setGenerateNullValue(false);
      assertFalse(JacksonUtils.toJson(value).contains("missing"));
      JacksonUtils.setGenerateNullValue(true);
      assertTrue(JacksonUtils.toJson(value).contains("\"missing\":null"));
      assertTrue(new String(JacksonUtils.toJsonBytes(value), StandardCharsets.UTF_8).contains("\"missing\":null"));
      assertTrue(JacksonUtils.getObjectMapper().writeValueAsString(value).contains("\"missing\":null"));
    } finally {
      JacksonUtils.generateNullValue = previous;
    }
  }
}
