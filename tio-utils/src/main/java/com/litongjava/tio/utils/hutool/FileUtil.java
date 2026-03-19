package com.litongjava.tio.utils.hutool;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

  /** 类Unix路径分隔符 */
  private static final char UNIX_SEPARATOR = '/';
  /** Windows路径分隔符 */
  private static final char WINDOWS_SEPARATOR = '\\';

  /**
   * 获取文件扩展名，扩展名不带“.”
   * 
   * @param file 文件
   * @return 扩展名
   */
  public static String extName(File file) {
    if (null == file) {
      return null;
    }
    if (file.isDirectory()) {
      return null;
    }
    return extName(file.getName());
  }

  /**
   * 获得文件的扩展名，扩展名不带“.”
   * 
   * @param fileName 文件名
   * @return 扩展名
   */
  public static String extName(String fileName) {
    if (fileName == null) {
      return null;
    }
    int index = fileName.lastIndexOf(".");
    if (index == -1) {
      return StrUtil.EMPTY;
    } else {
      String ext = fileName.substring(index + 1);
      // 扩展名中不能包含路径相关的符号
      return (ext.contains(String.valueOf(UNIX_SEPARATOR)) || ext.contains(String.valueOf(WINDOWS_SEPARATOR)))
          ? StrUtil.EMPTY
          : ext;
    }
  }

  /**
   * @param data
   * @param file
   * @author tanyaowu
   * @throws IOException
   */
  public static void writeBytes(byte[] data, File file) {
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // 获取全路径
    String canonicalPath = null;
    try {
      canonicalPath = file.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // 通过Files获取文件的输出流
    try (OutputStream fos = Files.newOutputStream(Paths.get(canonicalPath));) {
      fos.write(data);
      fos.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 
   * @param content
   * @param path
   * @param charset
   * @author tanyaowu
   * @throws UnsupportedEncodingException
   */
  public static void writeString(String content, String path, String charset) throws UnsupportedEncodingException {
    byte[] data = content.getBytes(charset);
    File file = new File(path);
    writeBytes(data, file);
  }

  public static void writeString(String content, String path) {
    File file = new File(path);
    writeString(content, file);
  }

  public static void writeString(String content, File file) {
    byte[] data = content.getBytes();
    writeBytes(data, file);
  }

  /**
   * 清空文件夹<br>
   * 注意：清空文件夹时不会判断文件夹是否为空，如果不空则递归删除子文件或文件夹<br>
   * 某个文件删除失败会终止删除操作
   * 
   * @param directory 文件夹
   * @return 成功与否
   * @throws IORuntimeException IO异常
   * @since 3.0.6
   */
  public static boolean clean(File directory) throws Exception {
    if (directory == null || directory.exists() == false || false == directory.isDirectory()) {
      return true;
    }

    final File[] files = directory.listFiles();
    for (File childFile : files) {
      boolean isOk = del(childFile);
      if (isOk == false) {
        // 删除一个出错则本次删除任务失败
        return false;
      }
    }
    return true;
  }

  /**
   * 删除文件或者文件夹<br>
   * 注意：删除文件夹时不会判断文件夹是否为空，如果不空则递归删除子文件或文件夹<br>
   * 某个文件删除失败会终止删除操作
   * 
   * @param file 文件对象
   * @return 成功与否
   * @throws IORuntimeException IO异常
   */
  public static boolean del(File file) throws Exception {
    if (file == null || false == file.exists()) {
      return false;
    }

    if (file.isDirectory()) {
      clean(file);
    }
    try {
      Files.delete(file.toPath());
    } catch (IOException e) {
      throw new Exception(e);
    }
    return true;
  }

  public static byte[] readBytes(File file) {
    Path fileLocation = file.toPath();// Paths.get(file);
    byte[] data = null;
    try {
      data = Files.readAllBytes(fileLocation);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return data;
  }

  public static String readString(File file) {
    byte[] data = readBytes(file);
    return new String(data);
  }

  public static String readUTF8String(File file) {
    byte[] data = readBytes(file);
    try {
      return new String(data, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 递归遍历目录以及子目录中的所有文件<br>
   * 如果提供file为文件，直接返回过滤结果
   * 
   * @param path       当前遍历文件或目录的路径
   * @param fileFilter 文件过滤规则对象，选择要保留的文件，只对文件有效，不过滤目录
   * @return 文件列表
   * @since 3.2.0
   */
  public static List<File> loopFiles(String path, FileFilter fileFilter) {
    return loopFiles(file(path), fileFilter);
  }

  /**
   * 递归遍历目录以及子目录中的所有文件<br>
   * 如果提供file为文件，直接返回过滤结果
   * 
   * @param file       当前遍历文件或目录
   * @param fileFilter 文件过滤规则对象，选择要保留的文件，只对文件有效，不过滤目录
   * @return 文件列表
   */
  public static List<File> loopFiles(File file, FileFilter fileFilter) {
    List<File> fileList = new ArrayList<File>();
    if (null == file) {
      return fileList;
    } else if (false == file.exists()) {
      return fileList;
    }

    if (file.isDirectory()) {
      final File[] subFiles = file.listFiles();
      if (subFiles != null && subFiles.length > 0) {
        for (File tmp : subFiles) {
          fileList.addAll(loopFiles(tmp, fileFilter));
        }
      }
    } else {
      if (null == fileFilter || fileFilter.accept(file)) {
        fileList.add(file);
      }
    }

    return fileList;
  }

  /**
   * 递归遍历目录以及子目录中的所有文件
   * 
   * @param path 当前遍历文件或目录的路径
   * @return 文件列表
   * @since 3.2.0
   */
  public static List<File> loopFiles(String path) {
    return loopFiles(file(path));
  }

  /**
   * 递归遍历目录以及子目录中的所有文件
   * 
   * @param file 当前遍历文件
   * @return 文件列表
   */
  public static List<File> loopFiles(File file) {
    return loopFiles(file, null);
  }

  /**
   * 创建File对象，自动识别相对或绝对路径，相对路径将自动从ClassPath下寻找
   * 
   * @param path 文件路径
   * @return File
   */
  public static File file(String path) {
    if (StrUtil.isBlank(path)) {
      throw new NullPointerException("File path is blank!");
    }
    return new File(path);
  }

  public static String readString(URL resource) {
    if (resource == null) {
      throw new RuntimeException();
    }

    StringBuilder text = new StringBuilder();
    try (InputStream inputStream = resource.openStream();
        //
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      char[] buffer = new char[1024];
      int bytesRead;
      while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
        text.append(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return text.toString();
  }

  public static byte[] readBytes(URL resource) {
    if (resource == null) {
      throw new RuntimeException("Resource not found");
    }

    try (InputStream inputStream = resource.openStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[1024 * 10];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * readURLAsLines
   * 
   * @param resource
   * @return
   */
  public static List<String> readURLAsLines(URL resource) {
    try (InputStream inputStream = resource.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return reader.lines().collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 复制目录
   * 
   * @param source    源目录
   * @param target    目标目录
   * @param overwrite 是否覆盖目标中已存在的同名文件/目录
   */
  public static void copyDirectory(Path source, Path target, boolean overwrite) throws IOException {
    if (!Files.exists(source)) {
      throw new NoSuchFileException(source.toString());
    }
    if (!Files.isDirectory(source)) {
      throw new IOException(source.toString());
    }

    // 若允许覆盖并且目标已存在，为避免目录结构冲突，先删掉目标目录
    if (overwrite && Files.exists(target)) {
      Files.walk(target).sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }

    // 开始复制
    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path rel = source.relativize(dir);
        Path destDir = target.resolve(rel);
        Files.createDirectories(destDir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path rel = source.relativize(file);
        Path dest = target.resolve(rel);
        if (overwrite) {
          Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
          // 不覆盖：若存在则跳过（也可以改成抛错）
          if (Files.notExists(dest)) {
            Files.copy(file, dest, StandardCopyOption.COPY_ATTRIBUTES);
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
