package com.litongjava.tio.utils.notification;

import java.util.HashMap;

import okhttp3.Response;

public class NotificationUtils {

  private static INotificationFactory factory = new WeComNotificationFectory();

  public static INotificationFactory getFactory() {
    return factory;
  }

  public static void setFactory(INotificationFactory ifactory) {
    factory = ifactory;
  }

  public static Response sendTextMsg(String string) {
    return factory.getNotifaction().sendTextMsg(string);
  }

  public static Response sendWarm(NotifactionWarmModel model) {
    return factory.getNotifaction().sendWarm(model);

  }

  public static Response send(String webHookUrl, HashMap<String, Object> reqMap) {
    return factory.getNotifaction().send(webHookUrl, reqMap);
  }

  public static Response send(HashMap<String, Object> reqMap) {
    return factory.getNotifaction().send(reqMap);
  }

}
