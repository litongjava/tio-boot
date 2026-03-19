package com.litongjava.tio.http.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 *
 * @author tanyaowu
 * 2017年8月10日 下午5:05:49
 */
public class HttpIpUtils {
  private static Logger log = LoggerFactory.getLogger(HttpIpUtils.class);
  /**
   * 如果是被代理了，获取客户端ip时，依次从下面这些头部中获取
   */
  private final static String[] HEADER_NAMES_FOR_REALIP = new String[] { "x-forwarded-for", "proxy-client-ip",
      "wl-proxy-client-ip", "x-real-ip" };

  /**
   * 
   * @param request
   * @return
   * @author tanyaowu
   */
  public static String getRealIp(HttpRequest request) {

    if (request.httpConfig == null) {
      return request.getRemote().getHost();
    }

    String headerName = null;
    String ip = null;
    for (String name : HEADER_NAMES_FOR_REALIP) {
      headerName = name;
      ip = request.getHeader(headerName);

      if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
        break;
      }
    }

    if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
      headerName = null;
      ip = request.getRemote().getHost();
    }

    if (ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    if (StrUtil.isBlank(ip)) {
      ip = request.getRemote().getHost();
    }

    return ip;
  }

  /**
   * 获取真实ip
   * @param channelContext
   * @param httpConfig
   * @param httpHeaders
   * @return
   * @author tanyaowu
   */
  public static String getRealIp(ChannelContext channelContext, HttpConfig httpConfig,
      Map<String, String> httpHeaders) {
    if (httpConfig == null) {
      return channelContext.getClientNode().getHost();
    }

    if (httpConfig.isProxied()) {
      String headerName = null;
      String ip = null;
      for (String name : HEADER_NAMES_FOR_REALIP) {
        headerName = name;
        ip = httpHeaders.get(headerName);

        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
          break;
        }
      }

      if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
        headerName = null;
        ip = channelContext.getClientNode().getHost();
      }

      if (ip.contains(",")) {
        if (log.isInfoEnabled()) {
          log.info("ip[{}], header name:{}", ip, headerName);

        }
        ip = ip.split(",")[0].trim();
      }
      return ip;
    } else {
      return channelContext.getClientNode().getHost();
    }
  }

  /**
   * 
   * @param str
   * @return
   */
  public static boolean isIp(String str) {
    if (str.length() < 7 || str.length() > 15 || "".equals(str)) {
      return false;
    }
    /** 
     * 判断IP格式和范围 
     */
    String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
    Pattern pat = Pattern.compile(rexp);
    Matcher mat = pat.matcher(str);
    boolean ipAddress = mat.find();
    return ipAddress;
  }

}