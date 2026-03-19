package com.litongjava.tio.http.common;

import java.util.concurrent.atomic.AtomicLong;

import com.litongjava.aio.AioId;

/**
 * @author tanyaowu
 * 2017年6月5日 上午10:44:26
 */
public class HttpId implements AioId {
  private static java.util.concurrent.atomic.AtomicLong seq = new AtomicLong();

  /**
   *
   * @author tanyaowu
   */
  public HttpId() {
  }

  /**
   * @return
   * @author tanyaowu
   */
  @Override
  public String id() {
    return seq.incrementAndGet() + "";
  }
}
