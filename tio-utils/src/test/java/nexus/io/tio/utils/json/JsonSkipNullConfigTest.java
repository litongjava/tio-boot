package nexus.io.tio.utils.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 全局开关 {@code tio.json.skipNull} 的行为
 *
 * <p>
 * 默认工厂是静态的、一个 JVM 只初始化一次，所以"开"与"关"两档不可能在同一个进程里都覆盖到 ——
 * 这里断言的是**与进用例时的实际表现一致**，于是同一个用例在
 * {@code -Dtio.json.skipNull=true} 与不传参两种运行下都成立，两档都验到：
 *
 * <pre>
 * java -cp &lt;classpath&gt; org.junit.runner.JUnitCore nexus.io.tio.utils.json.JsonSkipNullConfigTest
 * java -Dtio.json.skipNull=true -cp &lt;classpath&gt; org.junit.runner.JUnitCore nexus.io.tio.utils.json.JsonSkipNullConfigTest
 * </pre>
 *
 * <p>
 * <b>基准取"实际表现"而不是"读一次配置"</b>：配置有四条来源（命令行 / 配置文件 / JVM 参数 / 环境变量），
 * 而默认工厂可能是<b>静态初始化时就按配置建好</b>的 —— 这时候再去读配置并不能说明工厂现在是什么样。
 * 真正的事实来源是 {@link Json#isSkipNull()}（它按输出表现判断）。
 *
 * <p>
 * 每个用例进来自带清理（先还原上一次可能留下的包装），退出时也只还原自己装的那一层，
 * 所以用例之间没有顺序耦合。
 */
public class JsonSkipNullConfigTest {

  /** 进这个类时的实际表现:所有"还原"断言都以它为基准 */
  private static boolean baselineSkipNull;

  @BeforeClass
  public static void captureBaseline() {
    Json.uninstallSkipNull();
    baselineSkipNull = Json.isSkipNull();
  }

  @After
  public void restoreGlobalState() {
    Json.uninstallSkipNull();
    System.clearProperty(Json.KEY_SKIP_NULL);
  }

  private static Map<String, Object> mapWithNull() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "playwright-server");
    map.put("error", null);
    return map;
  }

  @Test
  public void configKeyIsTheDocumentedOne() {
    Json.uninstallSkipNull();
    assertEquals("tio.json.skipNull", Json.KEY_SKIP_NULL);
  }

  @Test
  public void outputMatchesTheSwitch() {
    Json.uninstallSkipNull();
    String json = JsonUtils.toJson(mapWithNull());
    if (baselineSkipNull) {
      assertFalse("这一档是跳过 null,输出就不该带 null,实际:" + json, json.contains("null"));
    } else {
      assertTrue("这一档是带 null 输出,实际:" + json, json.contains("null"));
    }
    assertTrue("非 null 字段任何配置下都要保留,实际:" + json, json.contains("playwright-server"));
  }

  /** {@code isSkipNull()} 要能如实回报当前默认工厂的表现（上层用它回报"到底谁在起作用"） */
  @Test
  public void isSkipNullReportsCurrentBehaviour() {
    Json.uninstallSkipNull();
    assertEquals(baselineSkipNull, Json.isSkipNull());
  }

  /** 跳过 null 只该管输出：解析路径不受影响 */
  @Test
  public void parsingStillReadsNullFields() {
    Json.uninstallSkipNull();
    Object parsed = Json.getJson().parse("{\"ok\":true,\"msg\":null}");
    assertTrue(parsed instanceof Map);
    assertTrue("被解析的报文里 null 字段要照旧读出来", ((Map<?, ?>) parsed).containsKey("msg"));
  }

  /**
   * 显式安装这条路：无论默认工厂有没有建好，调用后立刻就是"跳过 null"，且重复调用安全
   *
   * <p>
   * 存在的意义是启动顺序：配置项只在静态初始化时读一次，项目要到自己的启动钩子里才知道该不该开，
   * 那时配置已经晚了 —— 用这个方法直接把行为切过去。
   */
  @Test
  public void explicitInstallTurnsItOnAndIsIdempotent() {
    Json.uninstallSkipNull();
    assertTrue("调用后必须是跳过 null", Json.installSkipNull());
    assertTrue("再调一次也还是跳过 null（不会层层包装）", Json.installSkipNull());
    assertTrue("实际输出也要对得上", Json.isSkipNull());
    assertFalse("输出里不该再有 null", JsonUtils.toJson(mapWithNull()).contains("null"));
  }

  /**
   * 装上之后要能精确还原
   *
   * <p>
   * 这条不只是为了测试：项目里"先按配置装、后面某个调用点又需要带 null"时靠它回退。还的必须是
   * **安装之前那个工厂实例**，而不是新建一个不跳过 null 的工厂 —— 后者会把 provider、日期格式等
   * 之前的选择一起抹掉。
   */
  @Test
  public void uninstallRestoresTheFactoryFromBeforeInstall() {
    Json.uninstallSkipNull();
    IJsonFactory before = Json.getJsonFactory();

    Json.installSkipNull();
    assertTrue("装完应当是跳过 null", Json.isSkipNull());

    Json.uninstallSkipNull();
    assertSame("要还原成安装之前那个工厂实例", before, Json.getJsonFactory());
    assertEquals("行为也要回到进用例时的样子", baselineSkipNull, Json.isSkipNull());
  }
}
