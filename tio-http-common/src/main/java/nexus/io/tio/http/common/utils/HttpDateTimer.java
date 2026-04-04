package nexus.io.tio.http.common.utils;

import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.utils.SystemTimer;
import nexus.io.tio.utils.SystemTimer.TimerListener;
import nexus.io.tio.utils.hutool.DateUtil;

/**
 * 
 * @author tanyaowu 
 * 2018年6月17日 下午10:37:16
 */
public class HttpDateTimer {

  static {
    SystemTimer.addTimerListener(new TimerListener() {
      @Override
      public void onChange(long currTime) {
        httpDateString = DateUtil.httpDate(currTime);
        httpDateValue = HeaderValue.from(httpDateString);
      }
    });
  }

  private static volatile String httpDateString = DateUtil.httpDate();

  public static volatile HeaderValue httpDateValue = HeaderValue.from(httpDateString);

  public static String currDateString() {
    return httpDateString;
  }

  public static HeaderValue httpDateValue() {
    return httpDateValue;
  }
}
