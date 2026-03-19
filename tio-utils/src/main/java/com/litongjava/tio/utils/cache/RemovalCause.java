package com.litongjava.tio.utils.cache;
public enum RemovalCause {
  EXPLICIT, // 显式移除
  REPLACED, // 替换导致的移除
  EXPIRED, // 过期
  EVICTED // 驱逐（例如，由于缓存大小限制）
}