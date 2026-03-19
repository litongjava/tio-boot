package com.litongjava.tio.utils.lock;

/**
 * @author tanyaowu
 *
 */
public interface ReadLockHandler<T> {
  /**
   * 
   * @param t
   */
  public void handler(T t);

}
