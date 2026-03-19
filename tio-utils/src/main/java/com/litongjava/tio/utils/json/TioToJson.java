package com.litongjava.tio.utils.json;

public interface TioToJson<T> {
  void toJson(T value, int depth, JsonResult ret);
}