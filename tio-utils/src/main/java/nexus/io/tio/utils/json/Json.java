package nexus.io.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nexus.io.model.type.TioTypeReference;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * json string 与 object 互转抽象
 */
public abstract class Json {

  private static IJsonFactory defaultJsonFactory = buildFactory();

  /**
   * 当对象级的 datePattern 为 null 时使用 defaultDatePattern jfinal 2.1 版本暂定
   * defaultDatePattern 值为 null，即 jackson、fastjson 默认使用自己的 date 转换策略
   */
  private static String defaultDatePattern = "yyyy-MM-dd HH:mm:ss"; // null;
  // protected String timestampPattern = "yyyy-MM-dd HH:mm:ss";
  private static String timestampPattern = null;

  /**
   * Json 继承类优先使用对象级的属性 datePattern, 然后才是全局性的 defaultDatePattern
   */
  protected String datePattern = null;

  // long to string
  private static boolean longToString = true;

  public static void setDefaultJsonFactory(IJsonFactory defaultJsonFactory) {
    Objects.requireNonNull(defaultJsonFactory, "defaultJsonFactory can not be null");
    Json.defaultJsonFactory = defaultJsonFactory;
  }

  protected static IJsonFactory buildFactory() {
    IJsonFactory factory = buildDefaultFactory();
    // 全局开关:默认关,所以不配置的项目行为一点不变
    if (EnvUtils.getBoolean(KEY_SKIP_NULL, false)) {
      return new SkipNullFactory(factory);
    }
    return factory;
  }

  /**
   * 配置项:序列化时是否跳过 null 值字段(默认 false,即保持"带 null 输出"的原有行为)
   *
   * <p>
   * 打开之后,{@link #getJson()} 也返回跳过 null 的实现,于是所有走默认工厂的**输出**点(HTTP 响应体、
   * controller 返回值、业务代码里的 {@code Json.getJson().toJson(..)})都不再写 null 值字段。
   *
   * <p>
   * <b>影响范围是这个进程</b>:默认工厂是静态字段,同一个 JVM 里被依赖进来的模块会一起变;这也包括用
   * {@code Json.getJson()} 拼**出站请求体**的地方(通知、HTTP 工具等)。所以它**只该由项目的配置打开**
   * —— 每个项目一份配置,打开只影响自己那一个进程;框架本身默认关。需要"必须显式传 null"的报文时,
   * 不要打开这个开关,或在那几个调用点改用具体实现。
   *
   * <p>
   * 取值只认 {@code true}(忽略大小写),写错按"关"处理。
   */
  public static final String KEY_SKIP_NULL = "tio.json.skipNull";

  /** 按配置建默认工厂(与读配置解耦,便于测试) */
  private static IJsonFactory buildDefaultFactory() {
    String provider = EnvUtils.getStr("tio.json.provider");
    if (provider == null) {
      return new MixedJsonFactory();
    }
    if ("fastjson".equals(provider)) {
      return new FastJson2Factory();
    } else if ("gson".equals(provider)) {
      return new GsonFactory();
    } else if ("jackson".equals(provider)) {
      return new JacksonFactory();
    } else {
      return new MixedJsonFactory();
    }

  }

  /**
   * 只改输出、不改解析的包装工厂
   *
   * <p>
   * 包的必须是"当前"工厂({@link #getJsonFactory()}),这样 {@code tio.json.provider} 选定的实现、
   * 以及别的代码已经装上的工厂都不会被弄丢。解析路径一律委托 —— 跳过 null 只该管写出去的东西,
   * 读进来的报文怎么解析不受影响。
   */
  private static final class SkipNullFactory implements IJsonFactory {

    private final IJsonFactory delegate;

    SkipNullFactory(IJsonFactory delegate) {
      this.delegate = delegate;
    }

    @Override
    public Json getJson() {
      return delegate.getSkipNullJson();
    }

    @Override
    public Json getSkipNullJson() {
      return delegate.getSkipNullJson();
    }
  }

  /**
   * 当前默认工厂是否已经在跳过 null 值字段
   *
   * <p>
   * 判据是"输出的实际表现",所以对 fastjson / gson / jackson / 混合实现都成立,也覆盖了"配置打开"
   * 与"某个项目自己装过包装工厂"两种情况。用途是让上层能回报"到底谁在起作用",不必靠猜。
   */
  public static boolean isSkipNull() {
    Map<String, Object> probe = new java.util.LinkedHashMap<>();
    probe.put("k", null);
    return !getJson().toJson(probe).contains("null");
  }

  /**
   * 由代码显式打开"跳过 null 值字段"(与配置项 {@code tio.json.skipNull} 等效)
   *
   * <p>
   * 配置项是在**默认工厂初始化时**读的(静态初始化一个 JVM 只发生一次)。如果这个开关是启动过程中才
   * 确定的 —— 例如项目在自己的启动钩子里按项目配置决定 —— 那时再往配置里写就晚了:默认工厂可能已经建好。
   * 这个方法补上那条路:无论工厂有没有建好,调用它都能立刻把行为切过去,并且**重复调用安全**。
   *
   * <p>
   * 若这次确实是它装的包装,原来的工厂会被记下来,{@link #uninstallSkipNull()} 可以精确还原成
   * "这次调用之前"的样子;如果已经有别的包装在起作用(或者配置本来就打开了),它什么都不做,
   * {@code uninstallSkipNull()} 也就不会把别人的设置拆掉。
   *
   * @return 调用之后默认工厂是否处于"跳过 null"状态
   */
  public static boolean installSkipNull() {
    return installSkipNull(true);
  }

  /**
   * 由代码显式设置"序列化是否跳过 null 值字段"
   *
   * <p>
   * {@code skipNull=true} 与 {@link #installSkipNull()} 等价;{@code skipNull=false} 时,**只有**
   * 这次装的包装会被还原(见 {@link #uninstallSkipNull()}),不是本方法装的一律不动 —— 否则
   * "关掉"就变成了"拆掉别人的设置"。
   *
   * @return 调用之后默认工厂是否处于"跳过 null"状态
   */
  public static boolean installSkipNull(boolean skipNull) {
    if (skipNull) {
      if (!isSkipNull()) {
        installedByCode = getJsonFactory();
        setDefaultJsonFactory(new SkipNullFactory(installedByCode));
      }
    } else {
      uninstallSkipNull();
    }
    return isSkipNull();
  }

  /**
   * 还原由 {@link #installSkipNull()} 装上的包装
   *
   * <p>
   * 这里还的是**这次安装之前**那个工厂实例,而不是"新建一个不跳过 null 的工厂":后者会把调用方
   * 之前对 provider / 日期格式 / 自定义工厂的选择一起抹掉。没装过就什么都不做。
   *
   * @return 调用之后默认工厂是否处于"跳过 null"状态
   */
  public static boolean uninstallSkipNull() {
    IJsonFactory installed = installedByCode;
    if (installed != null) {
      installedByCode = null;
      setDefaultJsonFactory(installed);
    }
    return isSkipNull();
  }

  /** 由 {@link #installSkipNull()} 记下的"包装之前的工厂";为 null 表示当前没有它装的包装 */
  private static volatile IJsonFactory installedByCode;

  public static IJsonFactory getJsonFactory() {
    return defaultJsonFactory;
  }

  public static void setDefaultDatePattern(String defaultDatePattern) {
    Json.defaultDatePattern = defaultDatePattern;
  }

  public Json setDatePattern(String datePattern) {
    this.datePattern = datePattern;
    return this;
  }

  public String getDatePattern() {
    return datePattern;
  }

  public String getDefaultDatePattern() {
    return defaultDatePattern;
  }

  public static Json getJson() {
    return defaultJsonFactory.getJson();
  }

  public static Json getSkipNullJson() {
    return defaultJsonFactory.getSkipNullJson();
  }

  public static void setTimestampPattern(String timestampPattern) {
    Json.timestampPattern = timestampPattern;
  }

  public static String getTimestampPattern() {
    return timestampPattern;
  }

  public static boolean isLongToString() {
    return longToString;
  }

  public static void setLongToString(boolean longToString) {
    Json.longToString = longToString;
  }

  public abstract String toJson(Object object);

  public abstract byte[] toJsonBytes(Object object);

  public abstract Object parse(String stringValue);

  public abstract <T> T parse(String jsonString, Class<T> type);

  public abstract Object parseObject(String jsonString);

  public abstract Object parseArray(String jsonString);

  public abstract <T> List<T> parseArray(String str, Class<T> elementType);

  public abstract Map<?, ?> parseToMap(String json);

  public abstract <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType);

  public abstract <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType);

  public abstract <T> T parse(String body, Type type);

  public abstract <T> T parse(byte[] body, Type type);

  public abstract <T> T parse(String body, TioTypeReference<T> tioTypeReference);

}
