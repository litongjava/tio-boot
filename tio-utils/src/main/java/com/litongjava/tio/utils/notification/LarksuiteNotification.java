package com.litongjava.tio.utils.notification;

import java.util.Map;

import okhttp3.Response;

public class LarksuiteNotification implements INotification {

  @Override
  public Response send(Map<String, Object> reqMap) {
    return LarksuiteNotificationUtils.send(reqMap);
  }

  @Override
  public Response sendTextMsg(String string) {
    return LarksuiteNotificationUtils.sendTextMsg(string);
  }

  @Override
  public Response sendWarm(NotifactionWarmModel model) {
    return LarksuiteNotificationUtils.sendWarm(model);
  }

  @Override
  public Response send(String webHookUrl, Map<String, Object> reqMap) {
    return LarksuiteNotificationUtils.send(webHookUrl, reqMap);
  }

}