package com.litongjava.tio.boot.tesing;

import java.util.Arrays;
import java.util.List;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.constatns.ConfigKeys;
import com.litongjava.tio.utils.environment.EnvironmentUtils;
import com.litongjava.tio.utils.environment.PropUtils;
import com.litongjava.tio.utils.hutool.ResourceUtil;

public class TioBootTest {

  public static void before(String env){
    if (env == null) {
      env = EnvironmentUtils.get("app.env");
    }

    if (ResourceUtil.getResource(ConfigKeys.DEFAULT_CONFIG_FILE_NAME) != null) {
      PropUtils.use(ConfigKeys.DEFAULT_CONFIG_FILE_NAME, env);
    } else {
      if (env != null) {
        PropUtils.use("app-" + env + ".properties");
      }
    }
  }

  public static void before(Class<?>... classes){
    before(null, classes);
  }

  public static void before(String env, Class<?>... classes) {
    before(env);
    List<Class<?>> scannedClasses = Arrays.asList(classes);
    Aop.initAnnotation(scannedClasses);

  }

  public static void scan(Class<?>... primarySources) throws Exception {
    before();
    List<Class<?>> scannedClasses = Aop.scan(primarySources);
    Aop.initAnnotation(scannedClasses);
  }

}