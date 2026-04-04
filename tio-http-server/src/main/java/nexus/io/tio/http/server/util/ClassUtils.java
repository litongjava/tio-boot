package nexus.io.tio.http.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.model.upload.UploadFile;
import nexus.io.tio.utils.hutool.ClassUtil;

/**
 * @author tanyaowu
 * 2017年7月26日 下午6:46:11
 */
public class ClassUtils {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(ClassUtils.class);

  public static boolean isSimpleTypeOrArray(Class<?> clazz) {
    return ClassUtil.isSimpleTypeOrArray(clazz) || clazz.isAssignableFrom(UploadFile.class)
        || clazz.isAssignableFrom(UploadFile[].class);
  }

  /**
   *
   * @author tanyaowu
   */
  public ClassUtils() {
  }
}
