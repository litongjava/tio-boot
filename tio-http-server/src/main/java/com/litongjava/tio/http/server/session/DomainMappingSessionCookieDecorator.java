package com.litongjava.tio.http.server.session;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.hutool.ReUtil;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * @author tanyaowu 
 * 2017年10月11日 下午2:59:10
 */
public class DomainMappingSessionCookieDecorator implements SessionCookieDecorator {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(DomainMappingSessionCookieDecorator.class);

  /**
   * key:    (.)*(.tiocloud.com){1}
   * value : 替换原始domain的domain，譬如.tiocloud.com
   * 
   * 结果会把域名为www.tiocloud.com的cookie的域名替换成.tiocloud.com
   */
  private Map<String, String> domainMap = null;

  /**
   * 
   * @author: tanyaowu
   */
  public DomainMappingSessionCookieDecorator(Map<String, String> domainMap) {
    this.domainMap = domainMap;
  }

  protected DomainMappingSessionCookieDecorator() {

  }

  public void addMapping(String key, String value) {
    domainMap.put(key, value);
  }

  public void removeMapping(String key) {
    domainMap.remove(key);
  }

  /** 
   * @param sessionCookie
   * @author: tanyaowu
   */
  @Override
  public void decorate(Cookie sessionCookie, HttpRequest request, String domain) {
    Set<Entry<String, String>> set = domainMap.entrySet();
    String initDomain = sessionCookie.getDomain();
    for (Entry<String, String> entry : set) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (StrUtil.equalsIgnoreCase(key, initDomain) || ReUtil.isMatch(key, initDomain)) {
        sessionCookie.setDomain(value);
      }
    }
  }

  public static void main(String[] args) {
    boolean ss = ReUtil.isMatch("(.)*(.tiocloud.com){1}", ".tiocloud.com");
    System.out.println(ss);

    ss = ReUtil.isMatch("(.)*(.tiocloud.com){1}", "www.tiocloud.com");
    System.out.println(ss);

    ss = ReUtil.isMatch("(.)*(.tiocloud.com){1}", "www.xx.tiocloud.com");
    System.out.println(ss);
  }

}
