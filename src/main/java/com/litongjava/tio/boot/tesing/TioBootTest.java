package com.litongjava.tio.boot.tesing;

import java.util.Arrays;
import java.util.List;

import com.litongjava.constatns.ServerConfigKeys;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.utils.environment.EnvUtils;

public class TioBootTest {

  public static void load(String env) {
    if (env != null) {
      EnvUtils.set(ServerConfigKeys.APP_ENV, env);
    }
    EnvUtils.load();
  }

  public static void runWith(Class<?>... classes) {
    runWith(null, classes);
  }

  public static void runWith(String env, Class<?>... classes) {
    load(env);
    List<Class<?>> scannedClasses = Arrays.asList(classes);
    Aop.initAnnotation(scannedClasses);

  }

  public static void run(String env, Class<?>... primarySources) {
    load(env);
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

  public static void run(Class<?>... primarySources) {
    run(null, primarySources);
  }

}
