package nexus.io.tio.boot.http.utils;

import java.lang.reflect.Method;

import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.hutool.Validator;

import nexus.io.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.RequestLine;

/**
 * 
 * @author Tong Li
 *
 */
public class TioHttpControllerUtils {
  public static Method getActionMethod(HttpRequest request, RequestLine requestLine, HttpConfig httpConfig,
      //
      TioBootHttpControllerRouter router) {
    Method method = null;

    String path = requestLine.path;
    if (router != null) {
      method = router.getActionByPath(path, request.getMethod().toString(), request);
      if (method == null) {
        if ("/".equals(path)) {
          method = router.getActionByPath("", request.getMethod().toString(), request);
        }
      }
    }
    if (method == null) {
      if (StrUtil.isNotBlank(httpConfig.getWelcomeFile())) {
        if (StrUtil.endWith(path, "/")) {
          path = path + httpConfig.getWelcomeFile();
          requestLine.setPath(path);
          if (router != null) {
            method = router.getActionByPath(path, request.getMethod().toString(), request);
            path = requestLine.path;
          }
        }
      }
    }

    return method;
  }

  public static String getDomain(HttpRequest request) {
    String domain = request.getDomain();

    boolean isip = Validator.isIpv4(domain);
    if (!isip) {
      if (domain != null) {
        String[] dms = StrUtil.split(domain, ".");
        if (dms.length > 2) {
          domain = "." + dms[dms.length - 2] + "." + dms[dms.length - 1];
        }
      }
    }
    return domain;
  }

}
