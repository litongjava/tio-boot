package com.litongjava.tio.http.server.intf;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestLine;

/**
 * @author tanyaowu
 * 2017年7月25日 下午2:16:06
 */
public interface HttpRequestInterceptor {

  /**
   * 在执行HttpRequestHandler.handler()前会先调用这个方法<br>
   * 如果返回了HttpResponse对象，则后续都不再执行，表示调用栈就此结束<br>
   * @param request
   * @param requestLine
   * @param channelContext
   * @param httpResponse 从缓存中获取到的HttpResponse对象
   * @return
   * @throws Exception
   */
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse)
      throws Exception;

  /**
   * 在执行HttpRequestHandler.handler()后会调用此方法，业务层可以统一在这里给HttpResponse作一些修饰
   * @param request
   * @param requestLine
   * @param response
   * @param cost 本次请求耗时，单位：毫秒
   * @throws Exception
   */
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost)
      throws Exception;
}
