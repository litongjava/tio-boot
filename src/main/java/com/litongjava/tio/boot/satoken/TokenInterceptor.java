package com.litongjava.tio.boot.satoken;

import com.litongjava.jfinal.aop.Interceptor;
import com.litongjava.jfinal.aop.Invocation;
import com.litongjava.tio.boot.http.TioControllerContext;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;

import cn.dev33.satoken.stp.StpUtil;

public class TokenInterceptor implements Interceptor {

  public void intercept(Invocation inv) {
    if (StpUtil.isLogin()) {
      inv.invoke();
    } else {
      HttpResponse response = TioControllerContext.getResponse();
      response.setStatus(HttpResponseStatus.C401);
      inv.setReturnValue(response);
    }
  }
}