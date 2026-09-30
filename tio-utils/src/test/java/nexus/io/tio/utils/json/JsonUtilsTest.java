package nexus.io.tio.utils.json;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * JsonUtils 的两种输出：带 null 与跳过 null
 *
 * <p>
 * 默认行为不变：{@link Json#getJson()} 照旧输出 null 值字段（同一个框架下有多个项目，全局改默认工厂会
 * 波及其他项目，所以这里只钉住按调用点的能力）。需要「响应里不出现 null」时，由各自的业务项目在启动时
 * 用 {@link Json#setDefaultJsonFactory} 装上自己的包装工厂，或直接调用
 * {@link JsonUtils#toSkipNullJson(Object)}。
 */
public class JsonUtilsTest {

  private static Map<String, Object> mapWithNull() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "playwright-server");
    map.put("error", null);
    return map;
  }

  @Test
  public void testToJsonBytes() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("age", 18);
    map.put("name", "Tong Li");
    byte[] jsonBytes = JsonUtils.toJsonBytes(map);
    assertTrue(new String(jsonBytes).contains("Tong Li"));
  }

  @Test
  public void defaultJsonKeepsNullFields() {
    String json = JsonUtils.toJson(mapWithNull());
    assertTrue("默认行为不变：null 字段照旧输出，实际：" + json, json.contains("null"));
  }

  @Test
  public void skipNullJsonDropsNullFields() {
    String json = JsonUtils.toSkipNullJson(mapWithNull());
    assertFalse("跳过 null 的实现不该输出 null 字段，实际：" + json, json.contains("null"));
    assertTrue("非 null 字段要保留，实际：" + json, json.contains("playwright-server"));
  }
}
