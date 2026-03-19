package com.litongjava.tio.utils.type;

import com.litongjava.result.OkResult;

public class LongUtils {

  @SuppressWarnings("unchecked")
  public static OkResult<Long> valueOf(String strId) {
    try {
      return OkResult.ok(Long.valueOf(strId));
    } catch (Exception e) {
      return OkResult.exception(e);
    }
  }

  public static Long ifNull(Long i, Long defaultValue) {
    return i != null ? i : defaultValue;
  }

  public static Long ifNull(Long i, long defaultValue) {
    return i != null ? i : defaultValue;
  }

  public static Long ifNull(long i, long defaultValue) {
    return i != 0 ? i : defaultValue;
  }

}
