package com.litongjava.tio.utils.type;

import com.litongjava.result.OkResult;

public class IntUtils {

  @SuppressWarnings("unchecked")
  public static OkResult<Integer> valueOf(String strId) {
    try {
      return OkResult.ok(Integer.valueOf(strId));
    } catch (Exception e) {
      return OkResult.exception(e);
    }
  }
  
  public static Integer ifNull(Integer i, Integer defaultValue) {
    return i != null ? i : defaultValue;
  }

  public static Integer ifNull(Integer i, int defaultValue) {
    return i != null ? i : defaultValue;
  }

  public static Integer ifNull(int i, int defaultValue) {
    return i != 0 ? i : defaultValue;
  }
}
