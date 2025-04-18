package com.litongjava.tio.boot.druid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.alibaba.druid.stat.DruidStatService;
import com.alibaba.druid.support.http.util.IPAddress;
import com.alibaba.druid.support.http.util.IPRange;
import com.alibaba.druid.util.Utils;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.utils.HttpIpUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.hutool.StrUtil;

public class DruidStatHandler {

  private static final String PREFIX = "/druid";
  private static final String COOKIE_NAME = "DRUID-SESSION";

  private final DruidStatService statService = DruidStatService.getInstance();
  private final String resourcePath = "support/http/resources";

  private String jmxUrl;
  private String jmxUsername;
  private String jmxPassword;
  private MBeanServerConnection conn;

  private String loginUser;
  private String loginPass;
  private boolean resetEnable;
  private final List<IPRange> allowList = new ArrayList<>();
  private final List<IPRange> denyList = new ArrayList<>();
  private final boolean removeAdvertise;

  // 简单内存会话表
  private static final ConcurrentHashMap<String, String> sessionMap = new ConcurrentHashMap<>();

  public DruidStatHandler(DruidConfig config) {
    // 1. 基本配置
    this.loginUser = config.getLoginUsername();
    this.loginPass = config.getLoginPassword();
    this.resetEnable = config.isResetEnable();
    this.jmxUrl = config.getJmxUrl();
    this.jmxUsername = config.getJmxUsername();
    this.jmxPassword = config.getJmxPassword();

    // 2. 白/黑名单
    config.getAllowIps().forEach(ip -> allowList.add(new IPRange(ip)));
    config.getDenyIps().forEach(ip -> denyList.add(new IPRange(ip)));

    // 3. DruidStatService 设置
    statService.setResetEnable(resetEnable);

    // 4. 如有 JMX，尝试连接
    if (jmxUrl != null && !jmxUrl.isEmpty()) {
      try {
        initJmxConn();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // 5. 去广告开关
    this.removeAdvertise = config.isRemoveAdvertise();
  }

  /** 注册到 tio-boot HTTP 路由中 */
  public HttpResponse handle(HttpRequest request) throws Exception {
    String fullPath = request.getRequestLine().getPath(); // e.g. "/druid/index.html" or "/druid/user.json"
    if (!fullPath.startsWith(PREFIX)) {
      return null; // not mine, 放行给下一个 handler
    }
    HttpResponse response = TioRequestContext.getResponse();
    String subPath = fullPath.substring(PREFIX.length()); // "/index.html", "/submitLogin", "/stat.json?..."

    // IP 访问控制
    String remote = HttpIpUtils.getRealIp(request);
    if (!isPermitted(remote)) {
      return serveResource("/nopermit.html", response);
    }

    // 登录提交
    if ("/submitLogin".equals(subPath)) {
      String u = request.getParameter("loginUsername");
      String p = request.getParameter("loginPassword");
      if (loginUser.equals(u) && loginPass.equals(p)) {
        String sessionId = UUID.randomUUID().toString();
        sessionMap.put(sessionId, u);
        response.setHeader("Set-Cookie", COOKIE_NAME + "=" + sessionId + "; Path=/druid; HttpOnly; Max-Age=86400");
        response.setString("success");
        return response;
      } else {
        response.setString("error");
        return response;
      }
    }

    boolean loggedIn = false;
    String cookie = request.getHeader("cookie");
    if (cookie != null) {
      for (String token : cookie.split(";")) {
        token = token.trim();
        if (token.startsWith(COOKIE_NAME + "=")) {
          String sessionId = token.substring((COOKIE_NAME + "=").length());
          if (sessionMap.containsKey(sessionId)) {
            loggedIn = true;
          }
          break;
        }
      }
    }

    // 是否需要登录
    if (loginUser != null && !loggedIn && !subPath.equals("/login.html")
    //
        && !subPath.startsWith("/css") && !subPath.startsWith("/js") && !subPath.startsWith("/img")) {
      response.sendRedirect(PREFIX + "/login.html");
      return response;
    }

    // JSON 接口
    if (subPath.endsWith(".json")) {
      String queryString = request.getRequestLine().getQueryString();
      String url = subPath + (StrUtil.isNotBlank(queryString) ? ("?" + queryString) : "");
      String result = process(url);
      response.setString(result, "utf-8", "application/json; charset=utf-8");
      return response;
    }

    // 其余静态资源
    return serveResource(subPath, response);
  }

  /** 读类路径下 Druid 自带资源 */
  private HttpResponse serveResource(String path, HttpResponse response) throws IOException {
    if (path.equals("") || "/".equals(path)) {
      response.sendRedirect(PREFIX + "/index.html");
      return response;
    }

    String file = resourcePath + path;

    // —— 去广告逻辑：仅针对 common.js —— 
    if (removeAdvertise && file.endsWith("js/common.js")) {
      String js = Utils.readFromResource(file);
      // 正则去掉底部 banner
      js = js.replaceAll("<a.*?banner\".*?</a><br/>", "");
      js = js.replaceAll("powered.*?shrek\\.wang</a>", "");
      response.setString(js, "utf-8", "application/javascript; charset=utf-8");
      return response;
    }

    if (file.endsWith(".css")) {
      String txt = Utils.readFromResource(file);
      response.setString(txt, "utf-8", "text/css; charset=utf-8");
      return response;
    }
    if (file.endsWith(".js")) {
      String txt = Utils.readFromResource(file);
      response.setString(txt, "utf-8", "application/javascript; charset=utf-8");
      return response;
    }
    if (file.endsWith(".html")) {
      String txt = Utils.readFromResource(file);
      response.setString(txt, "utf-8", "text/html; charset=utf-8");
      return response;
    }
    if (file.endsWith(".jpg") || file.endsWith(".png")) {
      byte[] data = Utils.readByteArrayFromResource(file);
      return Resps.bytesWithContentType(response, data, file.endsWith(".png") ? "image/png" : "image/jpeg");
    }
    // 默认二进制流
    byte[] data = Utils.readByteArrayFromResource(file);
    return Resps.bytesWithContentType(response, data, "application/octet-stream");
  }

  /** 核心：本地 or 远程 JMX 调用 */
  private String process(String url) {
    try {
      if (jmxUrl == null) {
        return statService.service(url);
      }
      // 远程 JMX
      if (conn == null) {
        initJmxConn();
      }
      return getJmxResult(conn, url);
    } catch (Exception e) {
      return DruidStatService.returnJSONResult(DruidStatService.RESULT_CODE_ERROR, e.getMessage());
    }
  }

  private void initJmxConn() throws IOException {
    JMXServiceURL u = new JMXServiceURL(jmxUrl);
    Map<String, ?> env = null;
    if (jmxUsername != null) {
      String[] creds = new String[] { jmxUsername, jmxPassword };
      env = Collections.singletonMap(JMXConnector.CREDENTIALS, creds);
    }
    JMXConnector jmxc = JMXConnectorFactory.connect(u, env);
    conn = jmxc.getMBeanServerConnection();
  }

  private String getJmxResult(MBeanServerConnection c, String url) throws Exception {
    ObjectName name = new ObjectName(DruidStatService.MBEAN_NAME);
    return (String) c.invoke(name, "service", new Object[] { url }, new String[] { String.class.getName() });
  }

  private boolean isPermitted(String ip) {
    if (ip.contains(":")) {
      return "0:0:0:0:0:0:0:1".equals(ip) || (denyList.isEmpty() && allowList.isEmpty());
    }
    IPAddress addr = new IPAddress(ip);
    for (IPRange r : denyList)
      if (r.isIPAddressInRange(addr)) {
        return false;
      }

    if (!allowList.isEmpty()) {
      for (IPRange r : allowList) {
        if (r.isIPAddressInRange(addr)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }
}
