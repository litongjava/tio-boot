package com.litongjava.tio.boot.context;

/**
 * Shut是过去式,表示已经关闭
 * @author Tong Li
 *
 */
@FunctionalInterface
public interface ShutCallback {
  void afterStoped();
}
