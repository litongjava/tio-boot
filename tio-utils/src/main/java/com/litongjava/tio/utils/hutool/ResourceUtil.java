package com.litongjava.tio.utils.hutool;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import jodd.util.ClassLoaderUtil;

/**
 * 资源工具类
 */
public class ResourceUtil {

  private static String CLASSPATH_PRE = "classpath:";

  /**
   * 获取ClassPath绝对路径
   * @param path classpath路径
   * @return 绝对路径
   */
  public static String getAbsolutePath(String path) {
    return getDecodedPath(getResource(path));
  }

  /**
   * 获得资源相对路径对应的URL
   * 
   * @param path 资源相对路径
   * @param baseClass 基准Class，获得的相对路径相对于此Class所在路径，如果为{@code null}则相对ClassPath
   * @return {@link URL}
   */
  public static URL getResource(String path) {
    if (StrUtil.startWithIgnoreCase(path, CLASSPATH_PRE)) {
      path = path.substring(CLASSPATH_PRE.length());
    }
    return getClassLoader().getResource(path);
  }

  /**
   * 获取ClassPath下的资源做为流
   * 
   * @param path 相对于ClassPath路径，可以以classpath:开头
   * @return {@link InputStream}资源
   */
  public static InputStream getResourceAsStream(String path) {
    if (StrUtil.startWithIgnoreCase(path, CLASSPATH_PRE)) {
      path = path.substring(CLASSPATH_PRE.length());
    }
    return getClassLoader().getResourceAsStream(path);
  }

  /**
   * 获取{@link ClassLoader}<br>
   * 获取顺序如下：<br>
   * 
   * <pre>
   * 1、获取当前线程的ContextClassLoader
   * 2、获取{@link ClassLoaderUtil}类对应的ClassLoader
   * 3、获取系统ClassLoader（{@link ClassLoader#getSystemClassLoader()}）
   * </pre>
   * 
   * @return 类加载器
   */
  private static ClassLoader getClassLoader() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = ResourceUtil.class.getClassLoader();
      if (null == classLoader) {
        classLoader = ClassLoader.getSystemClassLoader();
      }
    }
    return classLoader;
  }

  /**
   * 从URL对象中获取不被编码的路径Path<br>
   * 对于本地路径，URL对象的getPath方法对于包含中文或空格时会被编码，导致本读路径读取错误。<br>
   * 此方法将URL转为URI后获取路径用于解决路径被编码的问题
   * 
   * @param url {@link URL}
   * @return 路径
   */
  private static String getDecodedPath(URL url) {
    if (null == url) {
      return null;
    }

    String path = null;
    try {
      // URL对象的getPath方法对于包含中文或空格的问题
      path = url.toURI().getPath();
    } catch (URISyntaxException e) {
      // ignore
    }
    return (null != path) ? path : url.getPath();
  }

  /**
   * 列出类路径下指定目录中的所有资源。
   * @param dirPath 目录路径，例如 "sql-templates"
   * @param fileExtension 文件扩展名，例如 ".sql"
   * @return 资源路径列表
   */
  public static List<URL> listResources(String dirPath, String fileExtension) {
    List<URL> result = new ArrayList<>();
    Enumeration<URL> dirUrls;
    try {
      dirUrls = Thread.currentThread().getContextClassLoader().getResources(dirPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to get resources for directory: " + dirPath, e);
    }

    while (dirUrls.hasMoreElements()) {
      URL dirUrl = dirUrls.nextElement();
      String protocol = dirUrl.getProtocol();

      if ("file".equals(protocol)) {
        String urlPath = dirUrl.getPath();
        if (System.getProperty("os.name").toLowerCase().contains("win") && urlPath.startsWith("/")) {
          urlPath = urlPath.substring(1);
        }
        Path dirPathObj = Paths.get(urlPath);
        try (Stream<Path> stream = Files.walk(dirPathObj)) {
          Iterator<Path> it = stream.iterator();
          while (it.hasNext()) {
            Path p = it.next();
            if (p.toString().endsWith(fileExtension)) {
              result.add(p.toUri().toURL());
            }
          }
        } catch (IOException e) {
          throw new RuntimeException("Failed to walk file tree for: " + dirUrl, e);
        }

      } else if ("jar".equals(protocol)) {
        // —— 新增：支持嵌套 JAR —— 
        try {
          // 1) 先拿到 JarURLConnection
          JarURLConnection conn = (JarURLConnection) dirUrl.openConnection();
          java.util.jar.JarFile outerJar = conn.getJarFile();
          String entryName = conn.getEntryName();
          // entryName 举例: "BOOT-INF/lib/tio-mail-wing-1.0.0.jar!/sql-templates"

          if (entryName != null && entryName.contains(".jar!/")) {
            // 2) 内嵌 Jar 的前半段（BOOT-INF/lib/tio-mail-wing-1.0.0.jar）
            String nestedJarEntry = entryName.substring(0, entryName.indexOf("!/"));
            JarEntry nested = outerJar.getJarEntry(nestedJarEntry);

            // 3) 抽取到临时文件
            try (InputStream is = outerJar.getInputStream(nested)) {
              Path tmp = Files.createTempFile("nested-", ".jar");
              Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
              // 4) 用标准 JarFile 扫描内部 JAR
              try (java.util.jar.JarFile innerJar = new java.util.jar.JarFile(tmp.toFile())) {
                scanJar(innerJar, dirPath, fileExtension, result);
              } finally {
                Files.deleteIfExists(tmp);
              }
            }
          } else {
            // 5) 普通单层 JAR
            scanJar(outerJar, dirPath, fileExtension, result);
          }
        } catch (IOException e) {
          throw new RuntimeException("Failed to read JAR resources for: " + dirUrl, e);
        }
      }
    }
    return result;
  }

  public static List<URL> listResources(String dirPath) {
    List<URL> result = new ArrayList<>();
    Enumeration<URL> dirUrls;
    try {
      dirUrls = Thread.currentThread().getContextClassLoader().getResources(dirPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to get resources for directory: " + dirPath, e);
    }

    while (dirUrls.hasMoreElements()) {
      URL dirUrl = dirUrls.nextElement();
      String protocol = dirUrl.getProtocol();

      if ("file".equals(protocol)) {
        // —— 不变 —— 扫“file:” 文件夹
        String urlPath = dirUrl.getPath();
        if (System.getProperty("os.name").toLowerCase().contains("win") && urlPath.startsWith("/")) {
          urlPath = urlPath.substring(1);
        }
        Path dirPathObj = Paths.get(urlPath);
        try (Stream<Path> stream = Files.walk(dirPathObj)) {
          stream.forEach(p -> {
            result.add(dirUrl);
          });
        } catch (IOException e) {
          throw new RuntimeException("Failed to walk file tree for: " + dirUrl, e);
        }

      } else if ("jar".equals(protocol)) {
        // —— 新增：支持嵌套 JAR —— 
        try {
          // 1) 先拿到 JarURLConnection
          JarURLConnection conn = (JarURLConnection) dirUrl.openConnection();
          java.util.jar.JarFile outerJar = conn.getJarFile();
          String entryName = conn.getEntryName();
          // entryName 举例: "BOOT-INF/lib/tio-mail-wing-1.0.0.jar!/sql-templates"

          if (entryName != null && entryName.contains(".jar!/")) {
            // 2) 内嵌 Jar 的前半段（BOOT-INF/lib/tio-mail-wing-1.0.0.jar）
            String nestedJarEntry = entryName.substring(0, entryName.indexOf("!/"));
            JarEntry nested = outerJar.getJarEntry(nestedJarEntry);

            // 3) 抽取到临时文件
            try (InputStream is = outerJar.getInputStream(nested)) {
              Path tmp = Files.createTempFile("nested-", ".jar");
              Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
              // 4) 用标准 JarFile 扫描内部 JAR
              try (java.util.jar.JarFile innerJar = new java.util.jar.JarFile(tmp.toFile())) {
                scanJar(innerJar, dirPath, result);
              } finally {
                Files.deleteIfExists(tmp);
              }
            }
          } else {
            // 5) 普通单层 JAR
            scanJar(outerJar, dirPath, result);
          }
        } catch (IOException e) {
          throw new RuntimeException("Failed to read JAR resources for: " + dirUrl, e);
        }
      }
    }
    return result;
  }

  public static void scanJar(JarFile jar, String dirPath, List<URL> result) {
    // 先拿到这个 JarFile 对应的 “jar:file:...!/" 前缀
    // jar.getName() 返回的是底层文件系统路径，比如 "D:\...\tio-mail-wing-1.0.0.jar"
    String jarFilePath = Paths.get(jar.getName()).toUri().toString();
    // 例： "file:/D:/.../tio-mail-wing-1.0.0.jar"
    String jarUrlPrefix = "jar:" + jarFilePath + "!/";

    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith(dirPath + "/") && !entry.isDirectory()) {

        // 拼成一个标准的 jar URL
        URL resourceUrl;
        try {
          resourceUrl = new URL(jarUrlPrefix + name);
          result.add(resourceUrl);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }

      }
    }
  }

  /**
   * 抽取出的公用方法：扫描一个 JarFile，把满足 dirPath/*.fileExtension 的 entry 加入 result
   */
  public static void scanJar(java.util.jar.JarFile jar, String dirPath, String fileExtension, List<URL> result) {

    // 先拿到这个 JarFile 对应的 “jar:file:...!/" 前缀
    // jar.getName() 返回的是底层文件系统路径，比如 "D:\...\tio-mail-wing-1.0.0.jar"
    String jarFilePath = Paths.get(jar.getName()).toUri().toString();
    // 例： "file:/D:/.../tio-mail-wing-1.0.0.jar"
    String jarUrlPrefix = "jar:" + jarFilePath + "!/";

    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith(dirPath + "/") && name.endsWith(fileExtension) && !entry.isDirectory()) {

        // 拼成一个标准的 jar URL
        URL resourceUrl;
        try {
          resourceUrl = new URL(jarUrlPrefix + name);
          result.add(resourceUrl);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }

      }
    }
  }

}
