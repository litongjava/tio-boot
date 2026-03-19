package com.litongjava.tio.core;

import com.litongjava.aio.AioId;

/**
 * @author tanyaowu
 * 2017年6月5日 上午10:31:40
 */
public class DefaultTAioId implements AioId {

  /**
   *
   * @author tanyaowu
   */
  public DefaultTAioId() {
  }

  /**
   * @return
   * @author tanyaowu
   */
  @Override
  public String id() {
    return java.util.UUID.randomUUID().toString();
  }
}
