package com.litongjava.tio.boot.tesing;

import java.util.Arrays;
import java.util.List;

import com.litongjava.constatns.ServerConfigKeys;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.environment.PropUtils;
import com.litongjava.tio.utils.hutool.ResourceUtil;

public class TioBootTest {

  public static void before(String env) {
    if (env == null) {
      // 从命令中获取参数
      env = EnvUtils.get("app.env");
    }

    if (ResourceUtil.getResource(ServerConfigKeys.DEFAULT_CONFIG_FILE_NAME) != null) {
      PropUtils.use(ServerConfigKeys.DEFAULT_CONFIG_FILE_NAME, env);
    } else {
      if (env != null) {
        PropUtils.use("app-" + env + ".properties");
      }
    }
  }

  public static void before(Class<?>... classes) {
    before(null, classes);
  }

  public static void before(String env, Class<?>... classes) {
    before(env);
    List<Class<?>> scannedClasses = Arrays.asList(classes);
    Aop.initAnnotation(scannedClasses);

  }

  public static void scan(String env, Class<?>... primarySources) {
    before(env);
    List<Class<?>> scannedClasses = null;
    try {
      scannedClasses = Aop.scan(primarySources);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (scannedClasses != null && scannedClasses.size() > 0) {
      Aop.initAnnotation(scannedClasses);
    }

  }

  public static void scan(Class<?>... primarySources) {
    scan(null, primarySources);
  }
}
