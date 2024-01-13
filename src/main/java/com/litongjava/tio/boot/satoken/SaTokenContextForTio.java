package com.litongjava.tio.boot.satoken;

import com.litongjava.tio.boot.http.TioControllerContext;

import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;

/**
 * Sa-Token 上线文处理器 [Jfinal 版本实现]
 */
public class SaTokenContextForTio implements SaTokenContext {
  /**
   * 获取当前请求的Request对象
   */
  @Override
  public SaRequest getRequest() {
    return new SaRequestForTioHttp(TioControllerContext.getRequest());
  }

  /**
   * 获取当前请求的Response对象
   */
  @Override
  public SaResponse getResponse() {
    return new SaResponseForTioHttp(TioControllerContext.getResponse());
  }

  /**
   * 获取当前请求的 [存储器] 对象
   */
  @Override
  public SaStorage getStorage() {
    return new SaStorageForTioHttp(TioControllerContext.getRequest());
  }

  /**
   * 校验指定路由匹配符是否可以匹配成功指定路径
   */
  @Override
  public boolean matchPath(String pattern, String path) {
    return PathAnalyzer.get(pattern).matches(path);
  }

  @Override
  public boolean isValid() {
    return SaTokenContext.super.isValid();
  }
}
