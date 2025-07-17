package com.litongjava.tio.boot.sender;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.utils.HttpIpUtils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.network.IpUtils;
import com.litongjava.tio.utils.notification.NotifactionWarmModel;

public class NotifactionWarmUtils {
  public static NotifactionWarmModel toWarmModel(String appGroupName, String warningName, String level, HttpRequest request, Throwable e) {
    String requestId = request.getChannelContext().getId();

    String requestLine = request.getRequestLine().toString();
    String host = request.getHost();
    String userId = request.getUserIdString();
    String bodyString = request.getBodyString();

    String realIp = HttpIpUtils.getRealIp(request);

    // 获取完整的堆栈跟踪
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    String stackTrace = sw.toString();

    NotifactionWarmModel model = new NotifactionWarmModel();

    String localIp = IpUtils.getLocalIp();
    String appName = EnvUtils.get(ServerConfigKeys.APP_NAME);
    model.setAppEnv(EnvUtils.env());

    model.setAppGroupName(appGroupName);

    model.setAppName(appName);

    model.setWarningName(warningName);

    model.setLevel(level);

    model.setDeviceName(localIp);
    model.setTime(ZonedDateTime.now());
    model.setRequestId(requestId);
    model.setUserIp(realIp);
    model.setUserId(userId);
    model.setHost(host);
    model.setReferer(request.getReferer());
    model.setUserAgent(request.getUserAgent());
    model.setRequestLine(requestLine);
    model.setRequestBody(bodyString);
    model.setStackTrace(stackTrace);
    return model;
  }

}
