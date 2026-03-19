package com.litongjava.tio.utils.environment;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import com.litongjava.tio.utils.hutool.ResourceUtil;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * PropUtils can load properties file from CLASSPATH or File object.
 */
public class PropUtils {

  private static String envKey = "app.env";

  private static Prop prop = null;
  private static final ConcurrentHashMap<String, Prop> cache = new ConcurrentHashMap<String, Prop>();

  private PropUtils() {
  }

  /**
   * 设置环境 key，PropKit 将通过该 key 获取环境 value。envKey 默认值为 "app.env"
   */
  public static void setEnvKey(String envKey) {
    PropUtils.envKey = envKey;
  }

  public static String getEnvKey() {
    return PropUtils.envKey;
  }

  public static String getEnv() {
    return getProp().get(envKey);
  }

  /**
   * Use the properties file. It will loading the properties file if not loading.
   * @see #use(String, String)
   */
  public static Prop use(String fileName) {
    return use(fileName, Prop.DEFAULT_ENCODING);
  }

  public static Prop use(String fileName, String env) {
    return use(fileName, env, Prop.DEFAULT_ENCODING);
  }

  /**
   * Use the properties file. It will loading the properties file if not loading.
   * <p>
   * Example:<br>
   * PropKit.use("config.txt", "UTF-8");<br>
   * PropKit.use("other_config.txt", "UTF-8");<br><br>
   * String userName = PropKit.get("userName");<br>
   * String password = PropKit.get("password");<br><br>
   *
   * userName = PropKit.use("other_config.txt").get("userName");<br>
   * password = PropKit.use("other_config.txt").get("password");<br><br>
   *
   * PropKit.use("com/jfinal/config_in_sub_directory_of_classpath.txt");
   *
   * @param fileName the properties file's name in classpath or the sub directory of classpath
   * @param encoding the encoding
   */
  private static Prop use(String fileName, Charset encoding) {
    return use(fileName, null, encoding);
  }

  public static Prop use(String fileName, String env, Charset encoding) {
    return cache.computeIfAbsent(fileName, key -> {
      Prop ret = new Prop(key, encoding);
      handleEnv(ret, key, env);
      if (PropUtils.prop == null) {
        PropUtils.prop = ret;
      }
      return ret;
    });
  }

  /**
   * 根据环境配置切换配置文件，便于项目在 dev、pro 等环境下部署
   * 例如：
   * 1： 假定 config.txt 中存在配置 app.env = pro
   * 2： PropKit.use("config.txt") 则会加载 config-pro.txt 中的配置
   */
  private static void handleEnv(Prop ret, String key) {
    handleEnv(ret, key, null);

  }

  private static void handleEnv(Prop result, String fileName, String env) {
    if (env == null) {
      env = result.get(envKey);
    }
    if (StrUtil.isNotBlank(env)) {
      int index = fileName.lastIndexOf('.');
      String envConfigName = fileName.substring(0, index) + "-" + env + fileName.substring(index);
      if (ResourceUtil.getResource(envConfigName) != null) {
        Prop envConfig = new Prop(envConfigName);
        result.append(envConfig); // 追加环境配置
      }

    }
  }

  /**
   * Use the properties file bye File object. It will loading the properties file if not loading.
   * @see #use(File, String)
   */
  public static Prop use(File file) {
    return use(file, Prop.DEFAULT_ENCODING);
  }

  /**
   * Use the properties file bye File object. It will loading the properties file if not loading.
   * <p>
   * Example:<br>
   * PropKit.use(new File("/var/config/my_config.txt"), "UTF-8");<br>
   * Strig userName = PropKit.use("my_config.txt").get("userName");
   *
   * @param file the properties File object
   * @param encoding the encoding
   */
  public static Prop use(File file, Charset encoding) {
    return cache.computeIfAbsent(file.getName(), key -> {
      Prop ret = new Prop(file, encoding);
      handleEnv(ret, key);
      if (PropUtils.prop == null) {
        PropUtils.prop = ret;
      }
      return ret;
    });
  }

  public static Prop useless(String fileName) {
    Prop previous = cache.remove(fileName);
    if (PropUtils.prop == previous) {
      PropUtils.prop = null;
    }
    return previous;
  }

  public static void clear() {
    prop = null;
    cache.clear();
  }

  public static Prop append(Prop prop) {
    synchronized (PropUtils.class) {
      if (PropUtils.prop != null) {
        PropUtils.prop.append(prop);
      } else {
        PropUtils.prop = prop;
      }
      return PropUtils.prop;
    }
  }

  public static Prop append(String fileName, Charset encoding) {
    return append(new Prop(fileName, encoding));
  }

  public static Prop append(String fileName) {
    return append(fileName, Prop.DEFAULT_ENCODING);
  }

  public static Prop appendIfExists(String fileName, Charset encoding) {
    try {
      return append(new Prop(fileName, encoding));
    } catch (Exception e) {
      return PropUtils.prop;
    }
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
    if (file.exists()) {
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
    return prop != null;
  }

  public static Prop getProp() {
    if (prop == null) {
      throw new IllegalStateException("Load propties file by invoking PropKit.use(String fileName) method first.");
    }
    return prop;
  }

  public static Prop getProp(String fileName) {
    return cache.get(fileName);
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
