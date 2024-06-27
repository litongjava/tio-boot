package com.litongjava.tio.boot.satoken;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.litongjava.tio.http.common.Cookie;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.application.ApplicationInfo;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.util.SaFoxUtil;

/**
 * 对 SaRequest 包装类的实现（Servlet 版）
 *
 * @author click33
 * @since 1.19.0
 */
public class SaRequestForTioHttp implements SaRequest {

  /**
   * 底层Request对象
   */
  protected HttpRequest request;

  /**
   * 实例化
   * @param request request对象
   */
  public SaRequestForTioHttp(HttpRequest request) {
    this.request = request;
  }

  /**
   * 获取底层源对象 
   */
  @Override
  public Object getSource() {
    return request;
  }

  /**
   * 在 [请求体] 里获取一个值 
   */
  @Override
  public String getParam(String name) {
    return request.getParameter(name);
  }

  /**
   * 获取 [请求体] 里提交的所有参数名称
   * @return 参数名称列表
   */
  @Override
  public List<String> getParamNames() {
    Enumeration<String> parameterNames = request.getParameterNames();
    List<String> list = new ArrayList<>();
    while (parameterNames.hasMoreElements()) {
      list.add(parameterNames.nextElement());
    }
    return list;
  }

  /**
   * 获取 [请求体] 里提交的所有参数
   * @return 参数列表
   */
  @Override
  public Map<String, String> getParamMap() {
    // 获取所有参数
    Map<String, Object[]> parameterMap = request.getParameterMap();
    Map<String, String> map = new LinkedHashMap<>(parameterMap.size());
    for (String key : parameterMap.keySet()) {
      Object[] values = parameterMap.get(key);
      map.put(key, String.valueOf(values[0]));
    }
    return map;
  }

  /**
   * 在 [请求头] 里获取一个值 
   */
  @Override
  public String getHeader(String name) {
    return request.getHeader(name);
  }

  /**
   * 在 [Cookie作用域] 里获取一个值 
   */
  @Override
  public String getCookieValue(String name) {
    Cookie[] cookies = request.getCookiesArray();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie != null && name.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  /**
   * 返回当前请求path (不包括上下文名称) 
   */
  @Override
  public String getRequestPath() {
    return ApplicationInfo.cutPathPrefix(request.getRequestURI());
  }

  /**
   * 返回当前请求的url，例：http://xxx.com/test
   * @return see note
   */
  public String getUrl() {
    String currDomain = SaManager.getConfig().getCurrDomain();
    if (!SaFoxUtil.isEmpty(currDomain)) {
      return currDomain + this.getRequestPath();
    }
    return request.getRequestURL().toString();
  }

  /**
   * 返回当前请求的类型 
   */
  @Override
  public String getMethod() {
    return request.getMethod();
  }

  /**
   * 转发请求 
   */
  @Override
  public Object forward(String path) {
    try {
      HttpResponse response = (HttpResponse) SaManager.getSaTokenContextOrSecond().getResponse().getSource();
      request.getRequestDispatcher(path).forward(request, response);
      return null;
    } catch (Exception e) {
      throw new SaTokenException(e).setCode(SaTokenErrorCode.CODE_20001);
    }
  }

}
