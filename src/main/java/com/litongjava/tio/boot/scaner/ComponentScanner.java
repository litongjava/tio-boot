package com.litongjava.tio.boot.scaner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.tio.boot.annotation.ComponentScan;

public class ComponentScanner {

  public static List<Class<?>> scan(Class<?>... primarySources) throws Exception {
    List<Class<?>> classes = new ArrayList<>();
    for (Class<?> primarySource : primarySources) {
      // 获取注解值
      ComponentScan componentScan = primarySource.getAnnotation(ComponentScan.class);
      String[] basePackages = componentScan.value();

      // 如果未指定包或者为默认值，则从当前包开始扫描
      if (basePackages == null || basePackages.length == 0 || (basePackages.length == 1 && basePackages[0].isEmpty())) {
        basePackages = new String[] { primarySource.getPackage().getName() };
      }

      for (String basePackage : basePackages) {
        classes.addAll(findClasses(basePackage));
      }
    }

    return classes;
  }

  private static List<Class<?>> findClasses(String basePackage) throws Exception {
    List<Class<?>> classes = new ArrayList<>();
    String path = basePackage.replace('.', '/');
    URL resource = Thread.currentThread().getContextClassLoader().getResource(path);

    if (resource == null) {
      throw new IllegalArgumentException("No directory found for package " + basePackage);
    }

    File directory = new File(resource.getFile());
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        classes.addAll(findClasses(basePackage + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        String className = basePackage + '.' + file.getName().substring(0, file.getName().length() - 6);
        classes.add(Class.forName(className));
      }
    }

    return classes;
  }
}
