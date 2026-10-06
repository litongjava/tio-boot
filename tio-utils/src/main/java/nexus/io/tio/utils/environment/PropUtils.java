package nexus.io.tio.utils.environment;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;


/**
 * PropUtils can load properties file from CLASSPATH or File object.
 */
public class PropUtils {

  private static String envKey = "app.env";
  private static final Prop prop = new Prop();
  private static volatile boolean loaded;
  private static final ConcurrentHashMap<String, Prop> cache = new ConcurrentHashMap<String, Prop>();

  private PropUtils() { }

  public static void setEnvKey(String key) { envKey = key; }
  public static String getEnvKey() { return envKey; }
  public static String getEnv() { return prop.get(envKey); }

  /** Load one file and append it to the aggregate. Environment selection belongs to EnvUtils. */
  public static Prop use(String fileName) {
    return use(fileName, Prop.DEFAULT_ENCODING);
  }

  /** @deprecated Use EnvUtils.load(env, filename) for environment-aware loading. */
  @Deprecated
  public static Prop use(String fileName, String env) {
    return use(fileName, env, Prop.DEFAULT_ENCODING);
  }

  private static synchronized Prop use(String fileName, Charset encoding) {
    if (!sourceExists(fileName)) {
      throw new IllegalArgumentException("Properties file not found: " + fileName);
    }
    String key = sourceKey(fileName, encoding);
    Prop single = cache.computeIfAbsent(key, ignored -> new Prop(fileName, encoding));
    append(single);
    return single;
  }

  /** @deprecated Use EnvUtils.load(env, filename) for environment-aware loading. */
  @Deprecated
  public static Prop use(String fileName, String env, Charset encoding) {
    if (env == null) { return use(fileName, encoding); }
    return EnvUtils.loadProperties(fileName, env, encoding);
  }

  public static Prop use(File file) { return use(file, Prop.DEFAULT_ENCODING); }

  public static synchronized Prop use(File file, Charset encoding) {
    String key = fileKey(file, encoding);
    Prop single = cache.computeIfAbsent(key, ignored -> new Prop(file, encoding));
    append(single);
    return single;
  }

  private static String fileKey(File file, Charset encoding) {
    try { return file.getCanonicalFile().toURI().toString() + "|" + encoding.name(); }
    catch (java.io.IOException error) { throw new IllegalArgumentException("Cannot resolve properties path", error); }
  }

  private static String sourceKey(String filename, Charset encoding) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) { loader = PropUtils.class.getClassLoader(); }
    java.net.URL resource = loader.getResource(filename);
    if (resource != null) { return resource.toExternalForm() + "|" + encoding.name(); }
    File file = new File(filename);
    return fileKey(file, encoding);
  }

  private static boolean sourceExists(String filename) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) { loader = PropUtils.class.getClassLoader(); }
    return loader.getResource(filename) != null || new File(filename).isFile();
  }

  /** Evict a single-file cache entry; already merged values remain until clear(). */
  public static synchronized Prop useless(String filename) {
    return cache.remove(sourceKey(filename, Prop.DEFAULT_ENCODING));
  }

  public static synchronized void clear() {
    prop.getProperties().clear();
    cache.clear();
    loaded = false;
  }

  /** Copy values without making the aggregate an alias of the supplied Prop. */
  public static synchronized Prop append(Prop values) {
    prop.append(values);
    loaded = true;
    return prop;
  }

  public static Prop append(String fileName, Charset encoding) {
    return append(new Prop(fileName, encoding));
  }

  public static Prop append(String fileName) {
    return append(fileName, Prop.DEFAULT_ENCODING);
  }

  public static Prop appendIfExists(String fileName, Charset encoding) {
    if (sourceExists(fileName)) {
      return append(new Prop(fileName, encoding));
    }
    return prop;
  }

  public static Prop appendIfExists(String fileName) {
    return appendIfExists(fileName, Prop.DEFAULT_ENCODING);
  }

  public static Prop append(File file, Charset encoding) {
    return append(new Prop(file, encoding));
  }

  public static Prop append(File file) {
    return append(file, Prop.DEFAULT_ENCODING);
  }

  public static Prop appendIfExists(File file, Charset encoding) {
    if (file.isFile()) {
      append(new Prop(file, encoding));
    }
    return PropUtils.prop;
  }

  public static Prop appendIfExists(File file) {
    return appendIfExists(file, Prop.DEFAULT_ENCODING);
  }

  /**
   * Use the first found properties file
   */
  public static Prop useFirstFound(String... fileNames) {
    for (String fn : fileNames) {
      try {
        return use(fn, Prop.DEFAULT_ENCODING);
      } catch (Exception ignored) {
      }
    }
    throw new IllegalArgumentException("没有配置文件可被使用");
  }

  public static boolean isLoad() {
    return loaded;
  }

  public static Prop getProp() { return prop; }

  public static Prop getProp(String filename) {
    return cache.get(sourceKey(filename, Prop.DEFAULT_ENCODING));
  }

  public static String get(String key) {
    return getProp().get(key);
  }

  public static String get(String key, String defaultValue) {
    return getProp().get(key, defaultValue);
  }

  public static Integer getInt(String key) {
    return getProp().getInt(key);
  }

  public static Integer getInt(String key, Integer defaultValue) {
    return getProp().getInt(key, defaultValue);
  }

  public static Long getLong(String key) {
    return getProp().getLong(key);
  }

  public static Long getLong(String key, Long defaultValue) {
    return getProp().getLong(key, defaultValue);
  }

  public static Double getDouble(String key) {
    return getProp().getDouble(key);
  }

  public static Double getDouble(String key, Double defaultValue) {
    return getProp().getDouble(key, defaultValue);
  }

  public static Boolean getBoolean(String key) {
    return getProp().getBoolean(key);
  }

  public static Boolean getBoolean(String key, Boolean defaultValue) {
    return getProp().getBoolean(key, defaultValue);
  }

  public static boolean containsKey(String key) {
    return getProp().containsKey(key);
  }
}
