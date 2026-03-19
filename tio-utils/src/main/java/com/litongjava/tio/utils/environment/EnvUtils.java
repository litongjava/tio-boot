package com.litongjava.tio.utils.environment;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.tio.utils.hutool.ResourceUtil;

public class EnvUtils {
  private static final Logger log = LoggerFactory.getLogger(EnvUtils.class);

  private static String[] args;
  private static Map<String, String> cmdArgsMap = new HashMap<>();
  private static Map<String, String> appMap = new HashMap<>();
  public static final String defaultFilename = "app.properties";
  public static final String envKey = "app.env";
  private static volatile boolean loaded = false;

  public static String[] getArgs() {
    return args;
  }

  public Map<String, String> getCmdArgsMap() {
    return cmdArgsMap;
  }

  public static boolean isDevMode() {
    URL resource = EnvUtils.class.getClassLoader().getResource("");
    if (resource != null && resource.toString().endsWith("/target/classes/")) {
      return true;
    }
    return false;
  }

  public static Map<String, String> buildCmdArgsMap(String[] args) {
    Map<String, String> result = new HashMap<>();
    EnvUtils.args = args;
    if (args != null) {
      for (String arg : args) {
        if (arg.startsWith("--")) {
          String[] parts = arg.substring(2).split("=", 2);
          if (parts.length == 2) {
            result.put(parts[0], parts[1]);
          }
        }
      }
    }
    cmdArgsMap = result;
    return result;
  }

  public static String getStr(String key) {
    String value = appMap.get(key);
    if (value != null) {
      return value;
    }
    // comamdn line
    value = cmdArgsMap.get(key);

    if (value != null) {
      return value;
    }

    // java env
    value = System.getProperty(key);
    if (value != null) {
      return value;
    }
    // system env
    value = System.getenv(key);
    if (value != null) {
      return value;
    }

    value = System.getenv(key.replace(".", "_").toUpperCase());
    if (value != null) {
      return value;
    }
    // config file
    if (PropUtils.isLoad()) {
      value = PropUtils.get(key);
    }
    return value;
  }

  /**
   * 
   * @param key
   * @return
   */
  public static String get(String key) {
    return getStr(key);
  }

  public static String getStr(String key, String defaultValue) {
    return get(key, defaultValue);
  }

  /**
   * 
   * @param key
   * @param defaultValue
   * @return
   */
  public static String get(String key, String defaultValue) {
    String value = get(key);
    if (value != null) {
      return value;
    } else {
      return defaultValue;
    }
  }

  /**
   * 
   * @param key
   * @return
   */
  public static int getInt(String key) {
    String value = getStr(key);
    if (value != null) {
      return Integer.valueOf(value);
    } else {
      return 0;
    }
  }

  /**
   * 
   * @param key
   * @param defaultValue
   * @return
   */
  public static int getInt(String key, int defaultValue) {
    String value = get(key);
    if (value != null) {
      return Integer.valueOf(value);
    } else {
      return defaultValue;
    }
  }

  /**
   * 
   * @param key
   * @return
   */
  public static Integer getInteger(String key) {
    String value = getStr(key);
    if (value != null) {
      return Integer.valueOf(value);
    } else {
      return null;
    }
  }

  /**
   * 
   * @param key
   * @param defaultValue
   * @return
   */
  public static Integer getInteger(String key, Integer defaultValue) {
    String value = get(key);
    if (value != null) {
      return Integer.valueOf(value);
    } else {
      return defaultValue;
    }
  }

  /**
   * 
   * @param key
   * @return
   */
  public static Long getLong(String key) {
    String value = getStr(key);
    if (value != null) {
      return Long.valueOf(value);
    } else {
      return null;
    }
  }

  /**
   * 
   * @param key
   * @param defaultValue
   * @return
   */
  public static Long getLong(String key, Long defaultValue) {
    String value = get(key);
    if (value != null) {
      return Long.valueOf(value);
    } else {
      return defaultValue;
    }
  }

  public static boolean getBoolean(String key) {
    return Boolean.parseBoolean(get(key));
  }

  public static boolean getBoolean(String key, boolean defaultValue) {
    String value = get(key);
    if (value != null) {
      return Boolean.parseBoolean(value);
    } else {
      return defaultValue;
    }
  }

  public static String getEnv() {
    return getStr(envKey);
  }

  public static String env() {
    return getStr(envKey);
  }

  public static boolean isDev() {
    return "dev".equals(getStr(envKey));
  }

  public static boolean isLocal() {
    return "local".equals(getStr(envKey));
  }

  public static boolean isTest() {
    return "test".equals(getStr(envKey));
  }

  public static boolean isProd() {
    return "prod".equals(getStr(envKey));
  }

  public static void use(String envName) {
    set(envKey, envName);
  }

  public static void useDev() {
    set(envKey, "dev");
  }

  public static void useLocal() {
    set(envKey, "local");
  }

  public static void useTest() {
    set(envKey, "test");
  }

  public static void useProd() {
    set(envKey, "prod");
  }

  public static void load(String fileName) {
    PropUtils.use(fileName);
  }

  public static void load(String env, String filename) {
    PropUtils.use(filename, env);
  }

  public static void set(String key, String value) {
    appMap.put(key, value);
  }

  public static void load() {
    if (!loaded) {
      loaded = true;
      String env = env();
      if (ResourceUtil.getResource(defaultFilename) != null) {
        // 主文件会自动加载从文件
        PropUtils.use(defaultFilename, env);
        log.info("load:{}", defaultFilename);
      } else {
        // 直接加载从文件
        if (env != null) {
          String fileName = "app-" + env + ".properties";
          log.info("load:{}", fileName);
          PropUtils.use(fileName);
        } else {
          // create file
          File file = new File(defaultFilename);
          if (file.exists()) {
            PropUtils.use(defaultFilename);
            log.info("load:{}", defaultFilename);
          }
        }
      }

      if (ResourceUtil.getResource(".env") != null) {
        log.info("load from classpath:{}", ".env");
        PropUtils.append(".env");
      }

      File file = new File(".env");
      if (file.exists()) {
        PropUtils.append(file);
        log.info("load from path:{}", ".env");
      }

      File secretsFile = new File("secrets.txt");
      if (secretsFile.exists()) {
        PropUtils.append(secretsFile);
        log.info("load from path:{}", "secrets.txt");
      }

      File my = new File("my.txt");
      if (my.exists()) {
        PropUtils.append(my);
        log.info("load from path:{}", "my.txt");
      }

      log.info("app.env:{} app.name:{}", env(), get(ServerConfigKeys.APP_NAME));
    }

  }

  public static void load(String[] args) {
    buildCmdArgsMap(args);
    load();
  }
}
