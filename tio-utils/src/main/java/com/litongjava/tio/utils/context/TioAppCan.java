package com.litongjava.tio.utils.context;

import com.litongjava.tio.utils.notification.NotifactionWarmModel;
import com.litongjava.tio.utils.notification.NotificationSender;

public class TioAppCan {

  private static TioAppCan me = new TioAppCan();

  public static TioAppCan me() {
    return me;
  }

  private TioAppCan() {
  }

  private NotificationSender notificationSender;


  public void clean() {
    me = new TioAppCan();
  }
  
  public boolean sendAsync(NotifactionWarmModel model) {
    if(notificationSender!=null) {
      return notificationSender.sendAsync(model);
    }
    return false;
  }

  public boolean send(NotifactionWarmModel model) {
    if(notificationSender!=null) {
      return notificationSender.send(model);
    }
    return false;
  }

  public NotificationSender getNotificationSender() {
    return notificationSender;
  }

  public void setNotificationSender(NotificationSender notificationSender) {
    this.notificationSender = notificationSender;
  }

}
