package com.litongjava.tio.utils.notification;

import java.util.Map;

import okhttp3.Response;

public class WeComNotification implements INotification {

  @Override
  public Response send(Map<String, Object> reqMap) {
    return WeComNotificationUtils.send(reqMap);
  }

  @Override
  public Response sendTextMsg(String string) {
    return WeComNotificationUtils.sendTextMsg(string);
  }

  @Override
  public Response sendWarm(NotifactionWarmModel model) {
    return WeComNotificationUtils.sendWarm(model);
  }

  @Override
  public Response send(String webHookUrl, Map<String, Object> reqMap) {
    return WeComNotificationUtils.send(webHookUrl, reqMap);
  }

}