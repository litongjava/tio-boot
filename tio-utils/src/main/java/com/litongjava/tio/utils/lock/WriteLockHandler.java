package com.litongjava.tio.utils.lock;

/**
 * @author tanyaowu
 *
 */
public interface WriteLockHandler<T> {

  /**
   * 
   * @param t
   */
  public void handler(T t);

}
