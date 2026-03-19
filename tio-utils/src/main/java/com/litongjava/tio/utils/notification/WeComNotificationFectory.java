package com.litongjava.tio.utils.notification;

public class WeComNotificationFectory implements INotificationFactory {

  @Override
  public INotification getNotifaction() {
    return new WeComNotification();
  }

}
