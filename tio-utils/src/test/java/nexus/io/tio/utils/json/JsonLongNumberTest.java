package nexus.io.tio.utils.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.testng.annotations.Test;

public class JsonLongNumberTest {
  public static class Schema {
    private final Long maximum = 9007199254740991L;
    private final Long minimum = Long.MIN_VALUE;
    private final String label = "9007199254740991";
    private final String description = null;
    private final List<Long> values = Arrays.asList(Long.MAX_VALUE, null, 9007199254740991L);
    public Long getMaximum() { return maximum; }
    public Long getMinimum() { return minimum; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public List<Long> getValues() { return values; }
  }

  @Test
  public void supportsAllConfiguredProvidersAndRetainsTheirDefaultBehavior() throws Exception {
    IJsonFactory previous = Json.getJsonFactory();
    try {
      for (IJsonFactory factory : Arrays.asList(new TioJsonFactory(), new MixedJsonFactory(),
          new FastJson2Factory(), new JacksonFactory(), new GsonFactory())) {
        Json.setDefaultJsonFactory(factory);
        preservesNumbersStringsAndNullValues();
        skipsNullFieldsWithoutChangingArrayElements();
        leavesGlobalPolicyAndConcurrentSerializationUnchanged();
        assertEquals("\"9007199254740991\"", Json.getJson().toJson(9007199254740991L, true));
      }
    } finally {
      Json.setDefaultJsonFactory(previous);
    }
  }

  @Test
  public void delegatesToCustomFactoriesForBothNullPolicies() {
    IJsonFactory previous = Json.getJsonFactory();
    IJsonFactory custom = new IJsonFactory() {
      public Json getJson() {
        return new TioJson() {
          @Override
          public String toJson(Object object, boolean longToString) {
            return "custom:" + super.toJson(object, longToString);
          }
        };
      }
      public Json getSkipNullJson() {
        return new TioJson(true) {
          @Override
          public String toJson(Object object, boolean longToString) {
            return "skip:" + super.toJson(object, longToString);
          }
        };
      }
    };
    try {
      Json.setDefaultJsonFactory(custom);
      assertEquals("custom:9007199254740991", JsonUtils.toJsonWithLongAsNumber(9007199254740991L));
      assertEquals("skip:9007199254740991", JsonUtils.toSkipNullJsonWithLongAsNumber(9007199254740991L));
      Json.installSkipNull();
      assertEquals("skip:9007199254740991", JsonUtils.toJsonWithLongAsNumber(9007199254740991L));
    } finally {
      Json.uninstallSkipNull();
      Json.setDefaultJsonFactory(previous);
    }
  }

  @Test
  public void preservesNumbersStringsAndNullValues() {
    String json = JsonUtils.toJsonWithLongAsNumber(new Schema());
    assertTrue(json, json.contains("\"maximum\":9007199254740991"));
    assertTrue(json, json.contains("\"minimum\":-9223372036854775808"));
    assertTrue(json, json.contains("\"label\":\"9007199254740991\""));
    assertTrue(json, json.contains("\"description\":null"));
    assertTrue(json, json.contains("[9223372036854775807,null,9007199254740991]"));
    assertEquals("null", JsonUtils.toJsonWithLongAsNumber(null));
  }

  @Test
  public void skipsNullFieldsWithoutChangingArrayElements() {
    String json = JsonUtils.toSkipNullJsonWithLongAsNumber(new Schema());
    assertFalse(json, json.contains("description"));
    assertTrue(json, json.contains("\"maximum\":9007199254740991"));
    assertTrue(json, json.contains("[9223372036854775807,null,9007199254740991]"));
  }

  @Test
  public void leavesGlobalPolicyAndConcurrentSerializationUnchanged() throws Exception {
    boolean before = Json.isLongToString();
    IJsonFactory factory = Json.getJsonFactory();
    String normal = JsonUtils.toJson(9007199254740991L);
    String skipNull = JsonUtils.toSkipNullJson(new Schema());
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      Callable<Void> numbers = () -> {
        for (int i = 0; i < 200; i++) {
          assertEquals("9007199254740991", JsonUtils.toJsonWithLongAsNumber(9007199254740991L));
        }
        return null;
      };
      Callable<Void> defaults = () -> {
        for (int i = 0; i < 200; i++) {
          assertEquals(normal, JsonUtils.toJson(9007199254740991L));
          assertEquals(skipNull, JsonUtils.toSkipNullJson(new Schema()));
        }
        return null;
      };
      for (Future<Void> result : executor.invokeAll(Arrays.asList(numbers, defaults, numbers, defaults))) {
        result.get();
      }
    } finally {
      executor.shutdownNow();
    }
    assertEquals(before, Json.isLongToString());
    assertEquals(factory, Json.getJsonFactory());
  }
}
