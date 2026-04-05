package nexus.io.tio.utils.reflicaiton;

public class ClassCheckUtils {

  /**
   * @param className
   * @return
   */
  public static boolean check(String className) {
    try {
      // 尝试加载类
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
