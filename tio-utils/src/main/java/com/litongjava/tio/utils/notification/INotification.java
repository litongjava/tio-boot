package com.litongjava.tio.utils.notification;

import java.util.Map;

import okhttp3.Response;

public interface INotification {

  public Response sendTextMsg(String string);

  public Response sendWarm(NotifactionWarmModel model);

  public Response send(String webHookUrl, Map<String, Object> reqMap);

  public Response send(Map<String, Object> reqMap);

}
